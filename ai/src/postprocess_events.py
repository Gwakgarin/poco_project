import json
from collections import Counter
from pathlib import Path
import re
import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parent.parent

INPUT_CSV = PROJECT_ROOT / "data" / "test_predictions.csv"
OUTPUT_EVENT_CSV = PROJECT_ROOT / "data" / "postprocessed_events.csv"
OUTPUT_SESSION_CSV = PROJECT_ROOT / "data" / "behavior_sessions.csv"

SEGMENT_SECONDS = 5

# 같은 세션으로 묶을 최대 간격
SESSION_GAP_SECONDS = {
    "washing_machine": 180,  # 세탁기는 길게 지속될 수 있음
    "vacuum": 60,
    "dishes": 60,
    "microwave": 30,
    "door_event": 30,
    "elevator": 60,
    "traffic": 60,
    "cooking": 120,
    "dish_washing": 120,
}

# 이벤트를 행동 후보로 매핑
EVENT_TO_BEHAVIOR = {
    "washing_machine": "laundry",
    "vacuum": "cleaning",
    "microwave": "meal_prep",
    "dishes": "dish_or_meal_related",
    "door_event": "entry_or_exit",
    "elevator": "movement",
    "traffic": "outing_related",
    "cooking": "meal_prep",
    "dish_washing": "dish_washing",
}


def extract_seg_index(split_file: str) -> int:
    """
    파일명에서 seg 번호 추출.
    예: test_elevator_seg003.wav -> 3
    """
    name = Path(split_file).name
    match = re.search(r"_seg(\d+)", name)
    if match:
        return int(match.group(1))
    return 0


def smooth_predictions(df: pd.DataFrame) -> pd.DataFrame:
    """
    앞/현재/뒤 예측 결과를 이용해 일시적인 오분류를 보정.
    기본 방식:
    - 이전 라벨과 다음 라벨이 같고
    - 현재 라벨만 다르면
    - 현재 라벨을 앞뒤 라벨로 보정
    """
    df = df.copy()
    df["smoothed_label"] = df["pred_label"]

    for raw_file, group in df.groupby("raw_file"):
        idx_list = list(group.index)

        for i in range(1, len(idx_list) - 1):
            prev_idx = idx_list[i - 1]
            curr_idx = idx_list[i]
            next_idx = idx_list[i + 1]

            prev_label = df.loc[prev_idx, "pred_label"]
            curr_label = df.loc[curr_idx, "pred_label"]
            next_label = df.loc[next_idx, "pred_label"]

            if prev_label == next_label and curr_label != prev_label:
                df.loc[curr_idx, "smoothed_label"] = prev_label

    return df


def create_sessions(df, gap_threshold=30):
    sessions = []

    current_session = {
        "start_sec": df.iloc[0]["start_sec"],
        "end_sec": df.iloc[0]["end_sec"],
        "events": [],
        "event_labels": []
    }

    for _, row in df.iterrows():
        label = row["smoothed_label"]
        start = row["start_sec"]
        end = row["end_sec"]

        # 현재 세션과 시간 차이
        gap = start - current_session["end_sec"]

        # gap이 작으면 같은 세션으로 묶음
        if gap <= gap_threshold:
            current_session["end_sec"] = end
            current_session["events"].append(row.to_dict())
            current_session["event_labels"].append(label)

        else:
            # representative event 계산
            labels = current_session["event_labels"]

            representative = get_representative_event(labels)

            current_session["representative_event"] = representative

            sessions.append(current_session)

            # 새 세션 시작
            current_session = {
                "start_sec": start,
                "end_sec": end,
                "events": [row.to_dict()],
                "event_labels": [label]
            }

    # 마지막 세션 처리
    labels = current_session["event_labels"]
    representative = get_representative_event(labels)

    current_session["representative_event"] = representative

    sessions.append(current_session)

    return pd.DataFrame(sessions)

def get_representative_event(labels):
    counter = Counter(labels)

    # 우선순위 기반
    priority = [
        "vacuum",
        "washing_machine",
        "elevator",
        "traffic",
        "microwave",
        "door_event",
        "dishes"
    ]

    for p in priority:
        if p in labels:
            return p

    # fallback
    return counter.most_common(1)[0][0]



def apply_sequence_rules(session_df: pd.DataFrame) -> pd.DataFrame:
    """
    세션 간 조합 규칙 적용.
    representative_event 기준으로 행동 확정.
    """

    if len(session_df) == 0:
        return session_df

    session_df = session_df.copy()

    # 기본 결과
    session_df["rule_result"] = session_df["representative_event"]

    for i, row in session_df.iterrows():

        event = row["representative_event"]

        labels = row["event_labels"]

        duration = row["end_sec"] - row["start_sec"]

        event_count = len(labels)

        # 세탁기 → laundry
        if event == "washing_machine":

            if event_count >= 6 or duration >= 30:

                session_df.loc[
                    i,
                    "rule_result"
                ] = "laundry_confirmed"

        # 청소기 → cleaning
        elif event == "vacuum":

            if event_count >= 4 or duration >= 20:

                session_df.loc[
                    i,
                    "rule_result"
                ] = "cleaning_confirmed"

        # 전자레인지 → meal prep
        elif event == "microwave":

            if event_count >= 2 or duration >= 10:

                session_df.loc[
                    i,
                    "rule_result"
                ] = "meal_prep_confirmed"

    # 외출 조합 규칙
    rows = list(session_df.iterrows())

    for idx_a, row_a in rows:

        if row_a["representative_event"] != "door_event":
            continue

        for idx_b, row_b in rows:

            if row_b["start_sec"] <= row_a["start_sec"]:
                continue

            gap = row_b["start_sec"] - row_a["end_sec"]

            if (
                gap <= 180
                and row_b["representative_event"]
                in ["elevator", "traffic"]
            ):

                session_df.loc[
                    idx_a,
                    "rule_result"
                ] = "outing_candidate"

                session_df.loc[
                    idx_b,
                    "rule_result"
                ] = "outing_candidate"

    return session_df


def main():
    if not INPUT_CSV.exists():
        raise FileNotFoundError(f"테스트 예측 CSV가 없습니다: {INPUT_CSV}")

    df = pd.read_csv(INPUT_CSV)

    required_cols = {"raw_file", "split_file", "pred_label"}
    missing = required_cols - set(df.columns)

    if missing:
        raise ValueError(f"필요한 컬럼이 없습니다: {missing}")

    # split 번호 기반 시간 계산
    df["seg_index"] = df["split_file"].apply(extract_seg_index)

    df["start_sec"] = df["seg_index"] * SEGMENT_SECONDS
    df["end_sec"] = df["start_sec"] + SEGMENT_SECONDS

    df = df.sort_values(
        ["raw_file", "seg_index"]
    ).reset_index(drop=True)

    # 1. 예측 보정
    smoothed_df = smooth_predictions(df)

    # 2. 세션 생성
    session_df = create_sessions(smoothed_df)

    # 3. 규칙 적용
    session_df = apply_sequence_rules(session_df)

    # CSV 저장
    smoothed_df.to_csv(
        OUTPUT_EVENT_CSV,
        index=False,
        encoding="utf-8-sig"
    )

    session_df.to_csv(
        OUTPUT_SESSION_CSV,
        index=False,
        encoding="utf-8-sig"
    )

    # =========================
    # JSON 저장용 구조 변환
    # =========================

    json_results = []

    for _, row in session_df.iterrows():

        session_data = {
            "start_sec": row["start_sec"],
            "end_sec": row["end_sec"],
            "representative_event": row["representative_event"],
            "rule_result": row["rule_result"],
            "event_labels": row["event_labels"]
        }

        json_results.append(session_data)

    # JSON 저장
    with open(
        "data/behavior_sessions.json",
        "w",
        encoding="utf-8"
    ) as f:

        json.dump(
            json_results,
            f,
            ensure_ascii=False,
            indent=4
        )

    print("\nJSON 저장 완료!")

    print("\n=== 보정된 이벤트 결과 ===")
    print(
        smoothed_df[[
            "raw_file",
            "split_file",
            "pred_label",
            "smoothed_label",
            "start_sec",
            "end_sec"
        ]].head(20)
    )

    print("\n=== 행동 세션 결과 ===")
    print(
        session_df[[
            "start_sec",
            "end_sec",
            "representative_event",
            "rule_result"
        ]]
    )

    print("\n저장 완료")
 


if __name__ == "__main__":
    main()