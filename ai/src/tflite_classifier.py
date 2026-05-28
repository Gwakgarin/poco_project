import json
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
)


PROJECT_ROOT = Path(__file__).resolve().parent.parent

TFLITE_MODEL_PATH = PROJECT_ROOT / "models" / "classifier.tflite"
LABEL_MAP_PATH = PROJECT_ROOT / "models" / "label_map.json"

# 이미 test_classifier.py에서 생성한 test embedding 사용
TEST_EMB_DIR = PROJECT_ROOT / "data" / "test_embeddings"
TEST_RAW_DIR = PROJECT_ROOT / "data" / "test_raw"
RESULT_CSV = PROJECT_ROOT / "data" / "tflite_predictions.csv"
SUPPORTED_EXTENSIONS = (".wav", ".mp3", ".m4a", ".flac", ".ogg")


def load_label_map(label_map_path: Path) -> dict[int, str]:
    with open(label_map_path, "r", encoding="utf-8") as f:
        label_map = json.load(f)

    # JSON key는 문자열로 저장되므로 int로 변환
    return {int(k): v for k, v in label_map.items()}


def load_tflite_interpreter(model_path: Path):
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("=== TFLite Model Info ===")
    print("Input details:", input_details)
    print("Output details:", output_details)

    return interpreter, input_details, output_details


def find_raw_file_name(true_label: str, raw_stem: str) -> str:
    raw_dir = TEST_RAW_DIR / true_label

    for ext in SUPPORTED_EXTENSIONS:
        raw_path = raw_dir / f"{raw_stem}{ext}"
        if raw_path.exists():
            return raw_path.name

    return raw_stem


def parse_embedding_info(emb_file: Path) -> dict[str, str]:
    rel_path = emb_file.relative_to(TEST_EMB_DIR)

    if len(rel_path.parts) < 3:
        raise ValueError(
            f"예상하지 못한 embedding 경로 구조입니다: {emb_file}\n"
            "예상 구조: test_embeddings/<true_label>/<raw_file_stem>/<split>.npy"
        )

    true_label = rel_path.parts[0]
    raw_stem = rel_path.parts[1]
    split_file = f"{emb_file.stem}.wav"
    raw_file = find_raw_file_name(true_label, raw_stem)

    return {
        "true_label": true_label,
        "raw_file": raw_file,
        "split_file": split_file,
        "embedding_file": str(emb_file.relative_to(PROJECT_ROOT)),
    }


def predict_tflite(
    interpreter,
    input_details,
    output_details,
    embedding: np.ndarray,
) -> tuple[int, float]:
    # shape: (1024,) -> (1, 1024)
    x = embedding.astype(np.float32).reshape(1, -1)

    expected_dim = int(input_details[0]["shape"][-1])
    if x.shape[-1] != expected_dim:
        raise ValueError(
            f"입력 shape 불일치: expected last dim {expected_dim}, got {x.shape}"
        )

    interpreter.set_tensor(input_details[0]["index"], x)
    interpreter.invoke()

    output = interpreter.get_tensor(output_details[0]["index"])[0] #각 클래스에 대해 낸 점수 배열

    pred_index = int(np.argmax(output)) #점수의 가장 큰 클래스의 인덱스 선택
    pred_score = float(np.max(output)) #그 클래의 confidence 점수

    return pred_index, pred_score


def main():
    if not TFLITE_MODEL_PATH.exists():
        raise FileNotFoundError(f"TFLite 모델 파일이 없습니다: {TFLITE_MODEL_PATH}")

    if not LABEL_MAP_PATH.exists():
        raise FileNotFoundError(f"label_map.json 파일이 없습니다: {LABEL_MAP_PATH}")

    if not TEST_EMB_DIR.exists():
        raise FileNotFoundError(f"테스트 embedding 폴더가 없습니다: {TEST_EMB_DIR}")

    label_map = load_label_map(LABEL_MAP_PATH)

    interpreter, input_details, output_details = load_tflite_interpreter(
        TFLITE_MODEL_PATH
    )

    emb_files = sorted(TEST_EMB_DIR.rglob("*.npy"))

    if not emb_files:
        raise ValueError(
            f"{TEST_EMB_DIR} 안에 .npy embedding 파일이 없습니다. "
            "먼저 python3 src/test_classifier.py 를 실행해 test embedding을 생성하세요."
        )

    print("\n=== TFLite Prediction Results ===")

    records = []

    for emb_file in emb_files:
        embedding = np.load(emb_file)

        pred_index, pred_score = predict_tflite(
            interpreter,
            input_details,
            output_details,
            embedding,
        )

        pred_label = label_map.get(pred_index, "UNKNOWN") #인덱스에 해당하는 라벨이 없으면 UNKNOWN으로 표시

        rel_path = emb_file.relative_to(TEST_EMB_DIR)

        record = parse_embedding_info(emb_file)
        record.update(
            {
                "pred_label": pred_label,
                "pred_score": pred_score,
            }
        )
        records.append(record)

        print(
            f"{rel_path} -> {pred_label} "
            f"(index={pred_index}, score={pred_score:.4f})"
        )

    result_df = pd.DataFrame(records)
    result_df.to_csv(RESULT_CSV, index=False, encoding="utf-8-sig")
    print(f"\nTFLite 예측 결과 저장 완료: {RESULT_CSV}")

    if result_df.empty:
        return

    y_true = result_df["true_label"]
    y_pred = result_df["pred_label"]
    labels = sorted(set(y_true.unique()) | set(y_pred.unique()))

    print("\n=== TFLite Segment-Level Metrics ===")
    print("Accuracy:", accuracy_score(y_true, y_pred))
    print("Macro F1:", f1_score(y_true, y_pred, average="macro", zero_division=0))
    print(
        "Weighted F1:",
        f1_score(y_true, y_pred, average="weighted", zero_division=0),
    )

    print("\n=== TFLite Classification Report (segment-level) ===")
    print(classification_report(y_true, y_pred, labels=labels, zero_division=0))

    print("\n=== TFLite Confusion Matrix (segment-level) ===")
    print(labels)
    print(confusion_matrix(y_true, y_pred, labels=labels))


if __name__ == "__main__":
    main()
