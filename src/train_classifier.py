import os
import numpy as np
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

CSV_PATH = "data/embedding_metadata.csv"
MODEL_DIR = "models"
MODEL_PATH = os.path.join(MODEL_DIR, "classifier.pkl")

def load_dataset(csv_path):
    df = pd.read_csv(csv_path)
    X = []
    y = []
    for _, row in df.iterrows():
        embedding = np.load(row["embedding_file"])
        X.append(embedding)
        y.append(row["label"])
    return np.array(X), np.array(y)

if __name__ == "__main__":
    os.makedirs(MODEL_DIR, exist_ok=True)

    X, y = load_dataset(CSV_PATH)
    print("X shape:", X.shape)
    print("y shape:", y.shape)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=42,
        stratify=y
    )
    
    model = RandomForestClassifier(random_state=42)
    model.fit(X_train, y_train)
    
    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print("Accuracy:", acc)
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))
    
    joblib.dump(model, MODEL_PATH)
    print(f"\n모델 저장 완료: {MODEL_PATH}")