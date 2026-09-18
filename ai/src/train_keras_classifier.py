import json
import re
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf

from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import StratifiedGroupKFold
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight


PROJECT_ROOT = Path(__file__).resolve().parent.parent

CSV_PATH = PROJECT_ROOT / "data" / "embedding_metadata.csv"
MODEL_DIR = PROJECT_ROOT / "models"

MODEL_PATH = MODEL_DIR / "classifier.keras"
LABEL_MAP_PATH = MODEL_DIR / "label_map.json"

INPUT_DIM = 1024
# 전체를 5등분해 1/5을 최종 평가용으로 뗀다 (= 기존 test_size 0.2와 동일 비율)
N_SPLITS = 5
# 남은 4/5에서 다시 1/8을 EarlyStopping 감시용 검증셋으로 뗀다 → 학습 70% / 검증 10% / 평가 20%.
# 검증을 20%까지 키우면 학습이 60%로 줄어 성능이 떨어지고, 8% 아래로 줄이면
# 검증셋이 작아져 EarlyStopping 판단이 시드마다 크게 흔들린다.
VAL_SPLITS = 8
RANDOM_STATE = 42
EPOCHS = 80
BATCH_SIZE = 32

SEGMENT_SUFFIX = re.compile(r"_seg\d+$")


def source_group(label: str, split_file: str) -> str:
    """세그먼트가 잘려 나온 원본 음원 하나를 가리키는 키.

    data/split_5s/tv/tv_01_seg003.wav -> tv/tv_01
    같은 원본에서 나온 조각들이 학습/검증 양쪽으로 흩어지지 않게 묶는 용도.
    """
    stem = Path(split_file).stem
    return f"{label}/{SEGMENT_SUFFIX.sub('', stem)}"


def load_dataset(csv_path: Path) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV 파일이 없습니다: {csv_path}")

    df = pd.read_csv(csv_path)

    required_cols = {"label", "embedding_file", "split_file"}
    missing = required_cols - set(df.columns)
    if missing:
        raise ValueError(f"{csv_path}에 필요한 컬럼이 없습니다: {missing}")

    embeddings = []
    labels = []
    groups = []
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
        groups.append(source_group(row["label"], row["split_file"]))

    if skipped:
        print(f"[주의] 사용할 수 없는 샘플 {skipped}개를 건너뜀")

    if not embeddings:
        raise ValueError(f"사용 가능한 임베딩이 없습니다: {csv_path}")

    return (
        np.array(embeddings, dtype=np.float32),
        np.array(labels),
        np.array(groups),
    )


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
    x_val: np.ndarray,
    x_test: np.ndarray,
    y_train: np.ndarray,
    y_val: np.ndarray,
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
        validation_data=(x_val, y_val),
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

    x, labels, groups = load_dataset(CSV_PATH)

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(labels)

    print("=== 데이터 확인 ===")
    print("X shape:", x.shape)
    print("y shape:", y.shape)
    print("원본 음원 수:", len(np.unique(groups)))
    print("\n라벨별 개수:")
    print(pd.Series(labels).value_counts())

    # 같은 원본 음원에서 잘린 세그먼트는 통째로 한쪽에만 들어가야 한다.
    # 무작위로 섞으면 같은 녹음의 조각이 학습/검증 양쪽에 걸쳐 성능이 부풀려진다.
    outer = StratifiedGroupKFold(
        n_splits=N_SPLITS,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    fit_idx, test_idx = next(outer.split(x, y, groups=groups))

    # 학습분에서 EarlyStopping 감시용 검증셋을 다시 떼어낸다. 이때도 원본 단위로 나눈다
    # (Keras의 validation_split은 셔플 없이 뒤쪽을 잘라내므로 여기서는 쓸 수 없다).
    inner = StratifiedGroupKFold(
        n_splits=VAL_SPLITS,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    inner_train, inner_val = next(
        inner.split(x[fit_idx], y[fit_idx], groups=groups[fit_idx])
    )
    train_idx = fit_idx[inner_train]
    val_idx = fit_idx[inner_val]

    # 분할 인덱스는 오름차순이라 학습분이 라벨 순서대로 뭉쳐 있다. model.fit이 배치를 섞어주긴
    # 하지만, 순서에 의존하는 설정(validation_split 등)을 다시 쓰게 될 때를 대비해 미리 섞어둔다.
    np.random.default_rng(RANDOM_STATE).shuffle(train_idx)

    x_train, y_train = x[train_idx], y[train_idx]
    x_val, y_val = x[val_idx], y[val_idx]
    x_test, y_test = x[test_idx], y[test_idx]

    split_groups = {
        "학습": set(groups[train_idx]),
        "검증": set(groups[val_idx]),
        "평가": set(groups[test_idx]),
    }

    print("\n=== 분할 결과 (원본 음원 단위) ===")
    for name, idx in (("학습", train_idx), ("검증", val_idx), ("평가", test_idx)):
        print(f"{name}: 세그먼트 {len(idx):>4}개 / 원본 {len(split_groups[name]):>3}개")

    for a, b in (("학습", "검증"), ("학습", "평가"), ("검증", "평가")):
        overlap = split_groups[a] & split_groups[b]
        if overlap:
            raise RuntimeError(
                f"데이터 누수: 원본 {len(overlap)}개가 {a}/{b} 양쪽에 포함됨"
            )
    print("겹치는 원본 음원 없음 — 누수 없이 분리됨")

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
        x_val=x_val,
        x_test=x_test,
        y_train=y_train,
        y_val=y_val,
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
