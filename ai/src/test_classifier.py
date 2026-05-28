import os
import pickle
import joblib
from pathlib import Path

import librosa
import numpy as np
import pandas as pd
import soundfile as sf
from sklearn.metrics import classification_report, confusion_matrix
import tensorflow as tf
import tensorflow_hub as hub


# 경로 설정

PROJECT_ROOT = Path(__file__).resolve().parent.parent

TEST_RAW_DIR = PROJECT_ROOT / "data" / "test_raw"
TEST_SPLIT_DIR = PROJECT_ROOT / "data" / "test_split_5s"
TEST_EMB_DIR = PROJECT_ROOT / "data" / "test_embeddings"
MODEL_PATH = PROJECT_ROOT / "models" / "classifier.pkl"
RESULT_CSV = PROJECT_ROOT / "data" / "test_predictions.csv"

TEST_SPLIT_DIR.mkdir(parents=True, exist_ok=True)
TEST_EMB_DIR.mkdir(parents=True, exist_ok=True)

SUPPORTED_EXTENSIONS = (".wav", ".mp3", ".m4a", ".flac", ".ogg")
TARGET_SR = 16000
SEGMENT_SECONDS = 5
MIN_LAST_SECONDS = SEGMENT_SECONDS

# YAMNet 로드
yamnet_model = hub.load("https://tfhub.dev/google/yamnet/1")


def split_audio_file(
    file_path: Path,
    output_dir: Path,
    target_sr: int = 16000,
    segment_seconds: int = 5,
    min_last_seconds: float = 5.0,
) -> list[Path]:
    """원본 오디오를 5초 단위로 분할하고 저장."""
    base_name = file_path.stem
    y, sr = librosa.load(str(file_path), sr=target_sr, mono=True)

    segment_length = target_sr * segment_seconds
    min_last_length = int(target_sr * min_last_seconds)

    total_length = len(y)
    num_full_segments = total_length // segment_length
    remainder = total_length % segment_length

    saved_files: list[Path] = []

    for i in range(num_full_segments):
        start = i * segment_length
        end = start + segment_length
        segment = y[start:end]

        out_name = f"{base_name}_seg{i:03d}.wav"
        out_path = output_dir / out_name
        sf.write(str(out_path), segment, target_sr)
        saved_files.append(out_path)

    if remainder >= min_last_length:
        start = num_full_segments * segment_length
        segment = y[start:]

        out_name = f"{base_name}_seg{num_full_segments:03d}.wav"
        out_path = output_dir / out_name
        sf.write(str(out_path), segment, target_sr)
        saved_files.append(out_path)

    return saved_files


def load_wav_16k_mono(file_path: Path) -> np.ndarray:
    y, sr = librosa.load(str(file_path), sr=16000, mono=True)
    return y.astype(np.float32)


def extract_embedding_mean(file_path: Path) -> np.ndarray:
    waveform = load_wav_16k_mono(file_path)
    scores, embeddings, spectrogram = yamnet_model(waveform)
    embedding_mean = tf.reduce_mean(embeddings, axis=0).numpy().astype(np.float32)
    return embedding_mean


def main():
    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"모델 파일이 없습니다: {MODEL_PATH}")

    if not TEST_RAW_DIR.exists():
        raise FileNotFoundError(f"테스트 원본 폴더가 없습니다: {TEST_RAW_DIR}")

    raw_files = [
    f for f in TEST_RAW_DIR.rglob("*")
    if f.is_file() and f.suffix.lower() in SUPPORTED_EXTENSIONS
]

    if not raw_files:
        raise ValueError(f"테스트할 오디오 파일이 없습니다: {TEST_RAW_DIR}")

    # 분류기 로드
    model = joblib.load(MODEL_PATH)

    records = []

    for raw_file in sorted(raw_files):
        print(f"\n[원본 파일] {raw_file.name}")

        rel_dir = raw_file.relative_to(TEST_RAW_DIR).parent
        true_label = raw_file.relative_to(TEST_RAW_DIR).parts[0]

        split_subdir = TEST_SPLIT_DIR / rel_dir / raw_file.stem
        split_subdir.mkdir(parents=True, exist_ok=True)

        emb_subdir = TEST_EMB_DIR / rel_dir / raw_file.stem
        emb_subdir.mkdir(parents=True, exist_ok=True)

        split_files = split_audio_file(
            raw_file,
            split_subdir,
            target_sr=TARGET_SR,
            segment_seconds=SEGMENT_SECONDS,
            min_last_seconds=MIN_LAST_SECONDS,
        )

        if not split_files:
            print(f"  -> 저장된 split 없음 (너무 짧을 수 있음): {raw_file.name}")
            continue

        file_preds = []

        for split_file in split_files:
            emb = extract_embedding_mean(split_file)

            emb_path = emb_subdir / f"{split_file.stem}.npy"
            np.save(str(emb_path), emb)

            pred_label = model.predict([emb])[0]

            # 일부 sklearn 모델은 predict_proba 지원
            pred_score = None
            if hasattr(model, "predict_proba"):
                proba = model.predict_proba([emb])[0]
                pred_score = float(np.max(proba))

            file_preds.append(pred_label)

            records.append({
                "true_label": true_label,
                "raw_file": raw_file.name,
                "split_file": split_file.name,
                "embedding_file": str(emb_path),
                "pred_label": pred_label,
                "pred_score": pred_score,
            })

            if pred_score is not None:
                print(f"  {split_file.name} -> {pred_label} ({pred_score:.4f})")
            else:
                print(f"  {split_file.name} -> {pred_label}")

        # 원본 파일 단위 다수결
        if file_preds:
            majority_pred = pd.Series(file_preds).mode()[0]
            print(f"[원본 파일 최종 예측] {raw_file.name} -> {majority_pred}")

    result_df = pd.DataFrame(records)

    if not result_df.empty:
        print("\n=== Test Classification Report (segment-level) ===")
        print(classification_report(
            result_df["true_label"],
            result_df["pred_label"],
            zero_division=0
        ))

        labels = sorted(result_df["true_label"].unique())

        print("\n=== Test Confusion Matrix (segment-level) ===")
        print(labels)
        print(confusion_matrix(
            result_df["true_label"],
            result_df["pred_label"],
            labels=labels
        ))

    result_df.to_csv(RESULT_CSV, index=False, encoding="utf-8-sig")
    print(f"\n테스트 결과 저장 완료: {RESULT_CSV}")


if __name__ == "__main__":
    main()

    #meal outing housework 
    #평균 데시벨 관측 