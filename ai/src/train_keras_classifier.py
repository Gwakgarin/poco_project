import json
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf

from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight


PROJECT_ROOT = Path(__file__).resolve().parent.parent

CSV_PATH = PROJECT_ROOT / "data" / "embedding_metadata.csv"
MODEL_DIR = PROJECT_ROOT / "models"

MODEL_PATH = MODEL_DIR / "classifier.keras"
LABEL_MAP_PATH = MODEL_DIR / "label_map.json"

INPUT_DIM = 1024
TEST_SIZE = 0.2
RANDOM_STATE = 42
EPOCHS = 80
BATCH_SIZE = 32


def load_dataset(csv_path: Path) -> tuple[np.ndarray, np.ndarray]:
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV 파일이 없습니다: {csv_path}")

    df = pd.read_csv(csv_path)

    required_cols = {"label", "embedding_file"}
    missing = required_cols - set(df.columns)
    if missing:
        raise ValueError(f"{csv_path}에 필요한 컬럼이 없습니다: {missing}")

    embeddings = []
    labels = []
    skipped = 0

    for _, row in df.iterrows():
        emb_path = PROJECT_ROOT / row["embedding_file"]
        if not emb_path.exists():
            skipped += 1
            continue

        embedding = np.load(emb_path).astype(np.float32)
        if embedding.shape != (INPUT_DIM,):
            print(f"[SKIP] 임베딩 shape 이상: {emb_path}, shape={embedding.shape}")
            skipped += 1
            continue

        embeddings.append(embedding)
        labels.append(row["label"])

    if skipped:
        print(f"[주의] 사용할 수 없는 샘플 {skipped}개를 건너뜀")

    if not embeddings:
        raise ValueError(f"사용 가능한 임베딩이 없습니다: {csv_path}")

    return np.array(embeddings, dtype=np.float32), np.array(labels)


def build_model(num_classes: int) -> tf.keras.Model:
    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(INPUT_DIM,)),
            tf.keras.layers.Dense(256, activation="relu"),
            tf.keras.layers.Dropout(0.3),
            tf.keras.layers.Dense(128, activation="relu"),
            tf.keras.layers.Dropout(0.3),
            tf.keras.layers.Dense(num_classes, activation="softmax"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def train_and_evaluate(
    x_train: np.ndarray,
    x_test: np.ndarray,
    y_train: np.ndarray,
    y_test: np.ndarray,
    class_weight: dict[int, float],
) -> dict:
    print("\n=== Keras classifier 학습 ===")

    model = build_model(num_classes=len(np.unique(y_train)))

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_accuracy",
            patience=10,
            restore_best_weights=True,
        )
    ]

    history = model.fit(
        x_train,
        y_train,
        validation_split=0.2,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        class_weight=class_weight,
        callbacks=callbacks,
        verbose=1,
    )

    y_prob = model.predict(x_test, verbose=0)
    y_pred = np.argmax(y_prob, axis=1)
    accuracy = accuracy_score(y_test, y_pred)

    print("\n=== 성능 결과 ===")
    print("Accuracy:", accuracy)
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, zero_division=0))

    model.save(MODEL_PATH)
    print(f"\nKeras 모델 저장 완료: {MODEL_PATH}")

    return {
        "accuracy": float(accuracy),
        "best_val_accuracy": float(max(history.history["val_accuracy"])),
        "epochs_ran": len(history.history["loss"]),
    }


def save_label_map(label_encoder: LabelEncoder) -> None:
    label_map = {
        int(index): label
        for index, label in enumerate(label_encoder.classes_)
    }

    with open(LABEL_MAP_PATH, "w", encoding="utf-8") as f:
        json.dump(label_map, f, ensure_ascii=False, indent=2)

    print(f"label_map 저장 완료: {LABEL_MAP_PATH}")


def main() -> None:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    tf.keras.utils.set_random_seed(RANDOM_STATE)

    x, labels = load_dataset(CSV_PATH)

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(labels)

    print("=== 데이터 확인 ===")
    print("X shape:", x.shape)
    print("y shape:", y.shape)
    print("\n라벨별 개수:")
    print(pd.Series(labels).value_counts())

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=y,
    )

    class_weights = compute_class_weight(
        class_weight="balanced",
        classes=np.unique(y_train),
        y=y_train,
    )
    class_weight = {
        int(class_index): float(weight)
        for class_index, weight in zip(np.unique(y_train), class_weights)
    }

    save_label_map(label_encoder)

    result = train_and_evaluate(
        x_train=x_train,
        x_test=x_test,
        y_train=y_train,
        y_test=y_test,
        class_weight=class_weight,
    )

    print("\n=== 최종 결과 ===")
    print(
        f"accuracy={result['accuracy']:.4f}, "
        f"best_val_accuracy={result['best_val_accuracy']:.4f}, "
        f"epochs={result['epochs_ran']}"
    )
    print(f"최종 Keras 모델 저장 완료: {MODEL_PATH}")


if __name__ == "__main__":
    main()
