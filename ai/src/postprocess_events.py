import json
import re
from pathlib import Path
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parent.parent

INPUT_CSV = PROJECT_ROOT / "data" / "tflite_predictions.csv"

# 지윤님 서버 기준 파일명으로 통일
OUTPUT_EVENT_LOG_CSV = PROJECT_ROOT / "data" / "postprocessed_events.csv"
OUTPUT_EVENT_SESSION_CSV = PROJECT_ROOT / "data" / "behavior_sessions.csv"
OUTPUT_EVENT_SESSION_JSON = PROJECT_ROOT / "data" / "behavior_sessions.json"

SEGMENT_SECONDS = 5


def extract_seg_index(split_file: str) -> int:
    match = re.search(r"_seg(\d+)", Path(split_file).name)
    return int(match.group(1)) if match else 0


def build_session_record(
    raw_file: str,
    start_sec: int,
    end_sec: int,
    event_label: str,
    scores: list,
) -> dict:

    duration_sec = end_sec - start_sec
    event_count = len(scores)

    numeric_scores = (
        pd.to_numeric(pd.Series(scores), errors="coerce")
        .dropna()
    )

    mean_score = (
        float(numeric_scores.mean())
        if not numeric_scores.empty
        else None
    )

    return {
        "raw_file": raw_file,
        "start_sec": start_sec,
        "end_sec": end_sec,
        "event_label": event_label,
        "event_count": event_count,
        "duration_sec": duration_sec,
        "mean_score": mean_score,
        "status": "event_only",
    }


def create_event_sessions(df: pd.DataFrame) -> pd.DataFrame:
    """
    같은 raw_file 안에서 같은 smoothed_label이 연속되면
    하나의 소리 이벤트 세션으로 묶는다.

    행동 확정은 하지 않고,
    classifier가 감지한 소리 이벤트만 시간 구간으로 정리한다.
    """

    sessions = []

    for raw_file, group in df.groupby("raw_file", sort=False):

        group = (
            group.sort_values("seg_index")
            .reset_index(drop=True)
        )

        current_label = None
        current_start = None
        current_end = None
        current_scores = []

        for _, row in group.iterrows():

            label = row["smoothed_label"]
            start_sec = int(row["start_sec"])
            end_sec = int(row["end_sec"])

            pred_score = row.get("pred_score")

            # 첫 이벤트
            if current_label is None:
                current_label = label
                current_start = start_sec
                current_end = end_sec
                current_scores = [pred_score]
                continue

            # 같은 라벨이면 이어붙임
            if label == current_label:
                current_end = end_sec
                current_scores.append(pred_score)
                continue

            # 라벨 바뀌면 이전 세션 저장
            sessions.append(
                build_session_record(
                    raw_file=raw_file,
                    start_sec=current_start,
                    end_sec=current_end,
                    event_label=current_label,
                    scores=current_scores,
                )
            )

            # 새 세션 시작
            current_label = label
            current_start = start_sec
            current_end = end_sec
            current_scores = [pred_score]

        # 마지막 세션 저장
        if current_label is not None:
            sessions.append(
                build_session_record(
                    raw_file=raw_file,
                    start_sec=current_start,
                    end_sec=current_end,
                    event_label=current_label,
                    scores=current_scores,
                )
            )

    return pd.DataFrame(sessions)


def main() -> None:

    if not INPUT_CSV.exists():
        raise FileNotFoundError(
            f"예측 CSV가 없습니다: {INPUT_CSV}"
        )

    df = pd.read_csv(INPUT_CSV)

    required_cols = {
        "raw_file",
        "split_file",
        "pred_label",
    }

    missing = required_cols - set(df.columns)

    if missing:
        raise ValueError(
            f"필요한 컬럼이 없습니다: {missing}"
        )

    # segment 번호 추출
    df["seg_index"] = (
        df["split_file"]
        .apply(extract_seg_index)
    )

    # 시간 계산
    df["start_sec"] = (
        df["seg_index"] * SEGMENT_SECONDS
    )

    df["end_sec"] = (
        df["start_sec"] + SEGMENT_SECONDS
    )

    # 정렬
    df = (
        df.sort_values(
            ["raw_file", "seg_index"]
        )
        .reset_index(drop=True)
    )

    # 실시간 흐름에 맞춰 과한 보정 없이
    # classifier 예측 그대로 사용
    df["smoothed_label"] = df["pred_label"]

    output_cols = [
        "raw_file",
        "split_file",
        "pred_label",
        "smoothed_label",
        "seg_index",
        "start_sec",
        "end_sec",
    ]

    # pred_score가 있으면 포함
    if "pred_score" in df.columns:
        output_cols.insert(4, "pred_score")

    # 이벤트 로그
    event_log_df = df[output_cols].copy()

    # 연속 이벤트 세션 생성
    session_df = create_event_sessions(
        event_log_df
    )

    # CSV 저장
    event_log_df.to_csv(
        OUTPUT_EVENT_LOG_CSV,
        index=False,
        encoding="utf-8-sig",
    )

    session_df.to_csv(
        OUTPUT_EVENT_SESSION_CSV,
        index=False,
        encoding="utf-8-sig",
    )

    # JSON 저장
    with open(
        OUTPUT_EVENT_SESSION_JSON,
        "w",
        encoding="utf-8",
    ) as f:

        json.dump(
            session_df.to_dict(
                orient="records"
            ),
            f,
            ensure_ascii=False,
            indent=4,
        )

    print("\n=== 5초 단위 소리 이벤트 로그 ===")
    print(event_log_df.head(20))

    print("\n=== 소리 이벤트 세션 결과 ===")
    print(session_df)

    print("\n저장 완료")
    print("이벤트 로그:", OUTPUT_EVENT_LOG_CSV)
    print("이벤트 세션:", OUTPUT_EVENT_SESSION_CSV)
    print("JSON:", OUTPUT_EVENT_SESSION_JSON)


if __name__ == "__main__":
    main()
