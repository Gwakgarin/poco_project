from pathlib import Path
import os

import librosa
import numpy as np
import soundfile as sf


# =========================
# 경로 설정
# =========================
PROJECT_ROOT = Path(__file__).resolve().parent.parent

# 원본 학습 split 데이터
INPUT_ROOT = PROJECT_ROOT / "data" / "split_5s"

# 증강 데이터 저장 위치
OUTPUT_ROOT = PROJECT_ROOT / "data" / "aug_split_5s"

SUPPORTED_EXTENSIONS = (".wav",)
TARGET_SR = 16000


# =========================
# 증강 설정
# =========================
GAIN_FACTORS = [0.7, 1.2]      # 소리 크기 변화
NOISE_STD = 0.003              # 약한 잡음 추가
SHIFT_SECONDS = [0.2, -0.2]    # 앞/뒤 시간 이동


def load_audio(file_path: Path, sr: int = 16000) -> np.ndarray:
    """오디오 파일을 16kHz mono로 불러온다."""
    y, _ = librosa.load(str(file_path), sr=sr, mono=True)
    return y.astype(np.float32)


def save_audio(file_path: Path, y: np.ndarray, sr: int = 16000):
    """오디오 파일 저장."""
    file_path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(file_path), y, sr)


def clip_audio(y: np.ndarray) -> np.ndarray:
    """오디오 값이 -1~1 범위를 넘지 않도록 제한."""
    return np.clip(y, -1.0, 1.0).astype(np.float32)


def augment_gain(y: np.ndarray, factor: float) -> np.ndarray:
    """볼륨 조절."""
    return clip_audio(y * factor)


def augment_noise(y: np.ndarray, noise_std: float) -> np.ndarray:
    """약한 랜덤 잡음 추가."""
    noise = np.random.normal(0, noise_std, size=len(y)).astype(np.float32)
    return clip_audio(y + noise)


def augment_shift(y: np.ndarray, sr: int, shift_seconds: float) -> np.ndarray:
    """시간축 이동."""
    shift_samples = int(sr * shift_seconds)
    return np.roll(y, shift_samples).astype(np.float32)


def main():
    if not INPUT_ROOT.exists():
        raise FileNotFoundError(f"입력 폴더가 없습니다: {INPUT_ROOT}")

    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

    total_original = 0
    total_created = 0

    print("=== 데이터 증강 시작 ===")
    print("입력 폴더:", INPUT_ROOT)
    print("출력 폴더:", OUTPUT_ROOT)

    for label in sorted(os.listdir(INPUT_ROOT)):
        input_label_dir = INPUT_ROOT / label

        if not input_label_dir.is_dir():
            continue

        output_label_dir = OUTPUT_ROOT / label
        output_label_dir.mkdir(parents=True, exist_ok=True)

        wav_files = [
            f for f in sorted(os.listdir(input_label_dir))
            if f.lower().endswith(SUPPORTED_EXTENSIONS)
        ]

        print(f"\n[{label}] 원본 파일 수: {len(wav_files)}")

        for fname in wav_files:
            input_path = input_label_dir / fname
            base_name = input_path.stem

            try:
                y = load_audio(input_path, sr=TARGET_SR)
            except Exception as e:
                print(f"[에러] 로드 실패: {input_path} / {e}")
                continue

            total_original += 1

            # 1. 볼륨 변화
            for factor in GAIN_FACTORS:
                tag = f"gain_{str(factor).replace('.', '_')}"
                output_path = output_label_dir / f"{base_name}_{tag}.wav"

                if output_path.exists():
                    continue

                y_aug = augment_gain(y, factor)
                save_audio(output_path, y_aug, TARGET_SR)
                total_created += 1

            # 2. 잡음 추가
            output_path = output_label_dir / f"{base_name}_noise.wav"

            if not output_path.exists():
                y_aug = augment_noise(y, NOISE_STD)
                save_audio(output_path, y_aug, TARGET_SR)
                total_created += 1

            # 3. 시간 이동
            for shift_sec in SHIFT_SECONDS:
                tag = f"shift_{str(shift_sec).replace('-', 'm').replace('.', '_')}"
                output_path = output_label_dir / f"{base_name}_{tag}.wav"

                if output_path.exists():
                    continue

                y_aug = augment_shift(y, TARGET_SR, shift_sec)
                save_audio(output_path, y_aug, TARGET_SR)
                total_created += 1

        print(f"[{label}] 증강 완료")

    print("\n=== 데이터 증강 완료 ===")
    print("원본 파일 수:", total_original)
    print("새로 생성된 증강 파일 수:", total_created)
    print("증강 데이터 저장 위치:", OUTPUT_ROOT)


if __name__ == "__main__":
    main()