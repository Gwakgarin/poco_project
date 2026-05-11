import os
from pathlib import Path

import numpy as np
import pandas as pd

from extract_embedding import extract_embedding_mean


PROJECT_ROOT = Path(__file__).resolve().parent.parent

SPLIT_ROOT = PROJECT_ROOT / "data" / "aug_split_5s"
EMB_ROOT = PROJECT_ROOT / "data" / "aug_embeddings"
CSV_PATH = PROJECT_ROOT / "data" / "aug_embedding_metadata.csv"

SUPPORTED_EXTENSIONS = (".wav",)


def build_embedding_dataset(split_root: Path, emb_root: Path, csv_path: Path):
    records = []

    if csv_path.exists():
        existing_df = pd.read_csv(csv_path)

        if len(existing_df) > 0:
            records.extend(existing_df.to_dict("records"))
            existing_keys = set(existing_df["split_file"])
            print(f"기존 CSV 불러옴: {len(existing_df)}개")
        else:
            existing_keys = set()
            print("기존 CSV가 비어 있음, 새로 시작")
    else:
        existing_keys = set()
        print("기존 CSV 없음, 새로 시작")

    if not split_root.exists():
        raise FileNotFoundError(f"증강 split 폴더가 없습니다: {split_root}")

    for label in sorted(os.listdir(split_root)):
        label_path = split_root / label

        if not label_path.is_dir():
            continue

        emb_label_dir = emb_root / label
        emb_label_dir.mkdir(parents=True, exist_ok=True)

        file_list = sorted(os.listdir(label_path))

        for fname in file_list:
            if not fname.lower().endswith(SUPPORTED_EXTENSIONS):
                continue

            split_file_path = label_path / fname
            split_file_key = str(split_file_path)

            if split_file_key in existing_keys:
                print(f"[SKIP-CSV] {split_file_path}")
                continue

            base_name = split_file_path.stem
            npy_path = emb_label_dir / f"{base_name}.npy"

            if npy_path.exists():
                print(f"[SKIP-NPY] {split_file_path}")
            else:
                try:
                    embedding = extract_embedding_mean(str(split_file_path))
                    np.save(npy_path, embedding)
                    print(f"[완료] {label}/{fname} -> {npy_path}")
                except Exception as e:
                    print(f"[에러] {split_file_path}: {e}")
                    continue

            record = {
                "label": label,
                "split_file": split_file_key,
                "embedding_file": str(npy_path)
            }

            records.append(record)
            existing_keys.add(split_file_key)

            pd.DataFrame(records).to_csv(csv_path, index=False, encoding="utf-8-sig")

    final_df = pd.DataFrame(records)
    final_df.to_csv(csv_path, index=False, encoding="utf-8-sig")

    return final_df


if __name__ == "__main__":
    embedding_df = build_embedding_dataset(
        split_root=SPLIT_ROOT,
        emb_root=EMB_ROOT,
        csv_path=CSV_PATH
    )

    print("\n=== 증강 임베딩 생성 결과 ===")
    print(embedding_df.head())
    print("총 개수:", len(embedding_df))

    if len(embedding_df) > 0:
        print("\n라벨별 개수:")
        print(embedding_df["label"].value_counts())