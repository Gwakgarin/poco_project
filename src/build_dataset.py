import os
import numpy as np
import pandas as pd
from extract_embedding import extract_embedding_mean

SPLIT_ROOT = "data/split_5s"
EMB_ROOT = "data/embeddings"
CSV_PATH = "data/embedding_metadata.csv"
SUPPORTED_EXTENSIONS = (".wav",)

def build_embedding_dataset(split_root, emb_root, csv_path):
    records = []

    if os.path.exists(csv_path):
        existing_df = pd.read_csv(csv_path)
        records.extend(existing_df.to_dict("records"))
        existing_keys = set(existing_df["split_file"])
        print(f"기존 CSV 불러옴: {len(existing_df)}개")
        
    else:
        existing_keys = set()
        print("기존 CSV 없음, 새로 시작")

    for label in os.listdir(split_root):
        label_path = os.path.join(split_root, label)
        if not os.path.isdir(label_path):
            continue

        emb_label_dir = os.path.join(emb_root, label)
        os.makedirs(emb_label_dir, exist_ok=True)
        file_list = sorted(os.listdir(label_path))

        for fname in file_list:
            if not fname.lower().endswith(SUPPORTED_EXTENSIONS):
                continue
            
            split_file_path = os.path.join(label_path, fname)

            if split_file_path in existing_keys:
                print(f"[SKIP-CSV] {split_file_path}")
                continue

            base_name = os.path.splitext(fname)[0]
            npy_path = os.path.join(emb_label_dir, base_name + ".npy")

            if os.path.exists(npy_path):
                print(f"[SKIP-NPY] {split_file_path}")
                embedding = np.load(npy_path)
            else:
                try:
                    embedding = extract_embedding_mean(split_file_path)
                    np.save(npy_path, embedding)
                    print(f"[완료] {label}/{fname} -> {npy_path}")
                except Exception as e:
                    print(f"[에러] {split_file_path}: {e}")
                    continue
                
            record = {
                "label": label,
                "split_file": split_file_path,
                "embedding_file": npy_path
            }
            records.append(record)
            existing_keys.add(split_file_path)

            pd.DataFrame(records).to_csv(csv_path, index=False)

    final_df = pd.DataFrame(records)
    final_df.to_csv(csv_path, index=False)
    return final_df

if __name__ == "__main__":
    embedding_df = build_embedding_dataset(
        split_root=SPLIT_ROOT,
        emb_root=EMB_ROOT,
        csv_path=CSV_PATH
    )
    
    print("\n=== 최종 결과 ===")
    print(embedding_df.head())
    print("총 개수:", len(embedding_df))