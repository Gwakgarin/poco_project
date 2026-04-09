import os
import numpy as np
import tensorflow as tf
import tensorflow_hub as hub
import librosa

yamnet_model = hub.load("https://tfhub.dev/google/yamnet/1")


def load_wav_16k_mono(file_path: str) -> np.ndarray:
    y, sr = librosa.load(file_path, sr=16000, mono=True)
    return y.astype(np.float32)


def extract_embedding_mean(file_path: str) -> np.ndarray:
    waveform = load_wav_16k_mono(file_path)
    scores, embeddings, spectrogram = yamnet_model(waveform)
    embedding_mean = tf.reduce_mean(embeddings, axis=0).numpy().astype(np.float32)
    return embedding_mean


def save_embedding_npy(file_path: str, output_path: str) -> None:
    embedding = extract_embedding_mean(file_path)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    np.save(output_path, embedding)
    print(f"저장 완료: {output_path}")
    print(f"embedding shape: {embedding.shape}")