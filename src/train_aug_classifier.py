from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC


PROJECT_ROOT = Path(__file__).resolve().parent.parent

BASE_CSV = PROJECT_ROOT / "data" / "embedding_metadata.csv"
AUG_CSV = PROJECT_ROOT / "data" / "aug_embedding_metadata.csv"

MODEL_DIR = PROJECT_ROOT / "models"
MODEL_PATH = MODEL_DIR / "classifier_aug.pkl"

TEST_SIZE = 0.2
RANDOM_STATE = 42


def load_dataset(csv_path: Path):
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV 파일이 없습니다: {csv_path}")

    df = pd.read_csv(csv_path)

    required_cols = {"label", "embedding_file"}
    missing = required_cols - set(df.columns)

    if missing:
        raise ValueError(f"{csv_path}에 필요한 컬럼이 없습니다: {missing}")

    X = []
    y = []

    skipped = 0

    for _, row in df.iterrows():
        emb_path = Path(row["embedding_file"])
        label = row["label"]

        if not emb_path.exists():
            skipped += 1
            continue

        emb = np.load(emb_path).astype(np.float32)

        if emb.shape != (1024,):
            print(f"[SKIP] 임베딩 shape 이상: {emb_path}, shape={emb.shape}")
            skipped += 1
            continue

        X.append(emb)
        y.append(label)

    if skipped > 0:
        print(f"[주의] {csv_path.name}에서 {skipped}개 샘플을 건너뜀")

    if len(X) == 0:
        raise ValueError(f"사용 가능한 임베딩이 없습니다: {csv_path}")

    return np.array(X, dtype=np.float32), np.array(y)


if __name__ == "__main__":
    X_base, y_base = load_dataset(BASE_CSV)
    X_aug, y_aug = load_dataset(AUG_CSV)

    X = np.concatenate([X_base, X_aug], axis=0)
    y = np.concatenate([y_base, y_aug], axis=0)

    print("=== 데이터 확인 ===")
    print("원본 X shape:", X_base.shape)
    print("증강 X shape:", X_aug.shape)
    print("전체 X shape:", X.shape)
    print("전체 y shape:", y.shape)

    print("\n라벨별 전체 개수:")
    print(pd.Series(y).value_counts())

    unique_labels = np.unique(y)

    if len(unique_labels) < 2:
        raise ValueError(f"분류기 학습에는 최소 2개 이상의 클래스가 필요합니다. 현재 클래스: {unique_labels}")

    class_counts = pd.Series(y).value_counts()

    stratify_arg = y
    if (class_counts < 2).any():
        stratify_arg = None
        print("\n[주의] 일부 클래스 샘플 수가 2개 미만이라 stratify 없이 분할합니다.")

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=stratify_arg
    )

    clf = make_pipeline(
        StandardScaler(),
        SVC(
            kernel="rbf",
            probability=True,
            class_weight="balanced",
            random_state=RANDOM_STATE
        )
    )

    print("\n=== 증강 데이터 포함 분류기 학습 시작 ===")
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)

    acc = accuracy_score(y_test, y_pred)

    print("\n=== 성능 결과 ===")
    print("Accuracy:", acc)

    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    print("\nConfusion Matrix:")
    labels = sorted(unique_labels)
    cm = confusion_matrix(y_test, y_pred, labels=labels)
    cm_df = pd.DataFrame(
        cm,
        index=[f"true_{label}" for label in labels],
        columns=[f"pred_{label}" for label in labels]
    )
    print(cm_df)

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(clf, MODEL_PATH)

    print(f"\n증강 학습 모델 저장 완료: {MODEL_PATH}")