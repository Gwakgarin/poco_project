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
    "vaccum": 60,
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
    "vaccum": "cleaning",
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


def create_sessions(df: pd.DataFrame) -> pd.DataFrame:
    """
    보정된 이벤트 라벨을 시간 기준으로 묶어 행동 세션 생성.
    """
    sessions = []

    for raw_file, group in df.groupby("raw_file"):
        group = group.sort_values("start_sec").reset_index(drop=True)

        current_session = None

        for _, row in group.iterrows():
            label = row["smoothed_label"]
            start_sec = row["start_sec"]
            end_sec = row["end_sec"]

            behavior = EVENT_TO_BEHAVIOR.get(label, "unknown")
            max_gap = SESSION_GAP_SECONDS.get(label, 60)

            if current_session is None:
                current_session = {
                    "raw_file": raw_file,
                    "behavior": behavior,
                    "main_event": label,
                    "start_sec": start_sec,
                    "end_sec": end_sec,
                    "event_count": 1,
                    "events": [label],
                }
                continue

            gap = start_sec - current_session["end_sec"]

            # 같은 이벤트이고 시간 간격이 기준 안이면 같은 세션으로 묶음
            if label == current_session["main_event"] and gap <= max_gap:
                current_session["end_sec"] = end_sec
                current_session["event_count"] += 1
                current_session["events"].append(label)
            else:
                sessions.append(current_session)
                current_session = {
                    "raw_file": raw_file,
                    "behavior": behavior,
                    "main_event": label,
                    "start_sec": start_sec,
                    "end_sec": end_sec,
                    "event_count": 1,
                    "events": [label],
                }

        if current_session is not None:
            sessions.append(current_session)

    session_df = pd.DataFrame(sessions)

    if len(session_df) > 0:
        session_df["duration_sec"] = session_df["end_sec"] - session_df["start_sec"]
        session_df["events"] = session_df["events"].apply(lambda x: ",".join(x))

    return session_df


def apply_sequence_rules(session_df: pd.DataFrame) -> pd.DataFrame:
    """
    세션 간 조합 규칙 적용.
    예:
    - door_event 이후 elevator/traffic 발생 → outing_candidate
    - washing_machine 반복/지속 → laundry_confirmed
    """
    if len(session_df) == 0:
        return session_df

    session_df = session_df.copy()
    session_df["rule_result"] = session_df["behavior"]

    for raw_file, group in session_df.groupby("raw_file"):
        group = group.sort_values("start_sec")

        for i, row in group.iterrows():
            event = row["main_event"]

            # 세탁기: washing_machine이 일정 시간 이상이거나 반복되면 세탁으로 확정
            if event == "washing_machine":
                if row["event_count"] >= 6 or row["duration_sec"] >= 30:
                    session_df.loc[i, "rule_result"] = "laundry_confirmed"

            # 청소기: vacuum/vaccum 반복 시 청소로 확정
            if event in ["vaccum", "vacuum"]:
                if row["event_count"] >= 4 or row["duration_sec"] >= 20:
                    session_df.loc[i, "rule_result"] = "cleaning_confirmed"

            # 전자레인지: 1회성 이벤트로 식사 준비 후보
            if event == "microwave":
                if row["event_count"] >= 2 or row["duration_sec"] >= 10:
                    session_df.loc[i, "rule_result"] = "meal_prep_confirmed"

        # door_event 이후 elevator 또는 traffic이 일정 시간 안에 오면 외출 후보
        rows = list(group.iterrows())
        for idx_a, row_a in rows:
            if row_a["main_event"] != "door_event":
                continue

            for idx_b, row_b in rows:
                if row_b["start_sec"] <= row_a["start_sec"]:
                    continue

                gap = row_b["start_sec"] - row_a["end_sec"]

                if gap <= 180 and row_b["main_event"] in ["elevator", "traffic"]:
                    session_df.loc[idx_a, "rule_result"] = "outing_candidate"
                    session_df.loc[idx_b, "rule_result"] = "outing_candidate"

    return session_df


def main():
    if not INPUT_CSV.exists():
        raise FileNotFoundError(f"테스트 예측 CSV가 없습니다: {INPUT_CSV}")

    df = pd.read_csv(INPUT_CSV)

    required_cols = {"raw_file", "split_file", "pred_label"}
    missing = required_cols - set(df.columns)
    if missing:
        raise ValueError(f"필요한 컬럼이 없습니다: {missing}")

    # split 번호 기반으로 시간 정보 생성
    df["seg_index"] = df["split_file"].apply(extract_seg_index)
    df["start_sec"] = df["seg_index"] * SEGMENT_SECONDS
    df["end_sec"] = df["start_sec"] + SEGMENT_SECONDS

    df = df.sort_values(["raw_file", "seg_index"]).reset_index(drop=True)

    # 1. 앞뒤 구간 기반 보정
    smoothed_df = smooth_predictions(df)

    # 2. 세션 생성
    session_df = create_sessions(smoothed_df)

    # 3. 세션 조합 규칙 적용
    session_df = apply_sequence_rules(session_df)

    smoothed_df.to_csv(OUTPUT_EVENT_CSV, index=False, encoding="utf-8-sig")
    session_df.to_csv(OUTPUT_SESSION_CSV, index=False, encoding="utf-8-sig")

    print("=== 보정된 이벤트 결과 ===")
    print(smoothed_df[["raw_file", "split_file", "pred_label", "smoothed_label", "start_sec", "end_sec"]].head(20))

    print("\n=== 행동 세션 결과 ===")
    print(session_df)

    print("\n저장 완료:")
 


if __name__ == "__main__":
    main()