import json
from pathlib import Path

import pandas as pd

from behavior_rules import MEAL_RULES

PROJECT_ROOT = Path(__file__).resolve().parent.parent

INPUT_SESSION_CSV = (
    PROJECT_ROOT / "data" / "behavior_sessions.csv"
)

OUTPUT_BEHAVIOR_JSON = (
    PROJECT_ROOT / "data" / "behavior_results.json"
)


def match_sequence(events, rule_sequence):

    matched = []

    rule_idx = 0

    for event in events:

        current_label = event["event_label"]

        if current_label == rule_sequence[rule_idx]:

            matched.append(event)

            rule_idx += 1

            if rule_idx >= len(rule_sequence):
                return matched

    return None


def infer_behaviors(session_df):

    behavior_results = []

    grouped = session_df.groupby(
        "raw_file",
        sort=False,
    )

    for raw_file, group in grouped:

        events = (
            group.sort_values("start_sec")
            .to_dict(orient="records")
        )

        for rule in MEAL_RULES:

            matched = match_sequence(
                events,
                rule["sequence"],
            )

            if matched is None:
                continue

            start_sec = matched[0]["start_sec"]
            end_sec = matched[-1]["end_sec"]

            behavior_results.append(
                {
                    "raw_file": raw_file,
                    "behavior": rule["behavior"],
                    "start_sec": start_sec,
                    "end_sec": end_sec,
                    "matched_events": [
                        x["event_label"]
                        for x in matched
                    ],
                    "confidence": 0.9,
                }
            )

    return behavior_results


def main():

    if not INPUT_SESSION_CSV.exists():
        raise FileNotFoundError(
            f"세션 CSV 없음: {INPUT_SESSION_CSV}"
        )

    session_df = pd.read_csv(
        INPUT_SESSION_CSV
    )

    behavior_results = infer_behaviors(
        session_df
    )

    with open(
        OUTPUT_BEHAVIOR_JSON,
        "w",
        encoding="utf-8",
    ) as f:

        json.dump(
            behavior_results,
            f,
            ensure_ascii=False,
            indent=4,
        )

    print("\n=== 행동 추론 결과 ===")

    for result in behavior_results:
        print(result)

    print("\n저장 완료")
    print(OUTPUT_BEHAVIOR_JSON)


if __name__ == "__main__":
    main()
