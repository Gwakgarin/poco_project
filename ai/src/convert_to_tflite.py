from pathlib import Path

import tensorflow as tf


PROJECT_ROOT = Path(__file__).resolve().parent.parent

KERAS_MODEL_PATH = PROJECT_ROOT / "models" / "classifier.keras"
TFLITE_MODEL_PATH = PROJECT_ROOT / "models" / "classifier.tflite"


def convert_to_tflite(
    keras_model_path: Path = KERAS_MODEL_PATH,
    tflite_model_path: Path = TFLITE_MODEL_PATH,
) -> None:
    if not keras_model_path.exists():
        raise FileNotFoundError(
            f"Keras 모델 파일이 없습니다: {keras_model_path}\n"
            "먼저 python3 src/train_keras_classifier.py 를 실행하세요."
        )

    model = tf.keras.models.load_model(keras_model_path)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    tflite_model = converter.convert()

    tflite_model_path.parent.mkdir(parents=True, exist_ok=True)
    with open(tflite_model_path, "wb") as f:
        f.write(tflite_model)

    print(f"TFLite 변환 완료: {tflite_model_path}")


if __name__ == "__main__":
    convert_to_tflite()
