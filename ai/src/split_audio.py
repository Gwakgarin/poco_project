import os
import librosa
import soundfile as sf

INPUT_ROOT = "data/raw"
OUTPUT_ROOT = "data/split_5s"

TARGET_SR = 16000
SEGMENT_SECONDS = 5
MIN_LAST_SECONDS = SEGMENT_SECONDS
SUPPORTED_EXTENSIONS = (".wav", ".mp3", ".m4a", ".flac", ".ogg")

def split_audio_file(file_path, output_dir, target_sr=16000, segment_seconds=5, min_last_seconds=None):
    base_name = os.path.splitext(os.path.basename(file_path))[0]
    y, sr = librosa.load(file_path, sr=target_sr, mono=True)
    if min_last_seconds is None:
        min_last_seconds = segment_seconds
    segment_length = target_sr * segment_seconds
    min_last_length = int(target_sr * min_last_seconds)
    total_length = len(y)
    num_full_segments = total_length // segment_length
    remainder = total_length % segment_length
    saved_files = []

    for i in range(num_full_segments):
        start = i * segment_length
        end = start + segment_length
        segment = y[start:end]
        out_name = f"{base_name}_seg{i:03d}.wav"
        out_path = os.path.join(output_dir, out_name)
        sf.write(out_path, segment, target_sr)
        saved_files.append(out_path)

    if remainder >= min_last_length:
        start = num_full_segments * segment_length
        segment = y[start:]
        out_name = f"{base_name}_seg{num_full_segments:03d}.wav"
        out_path = os.path.join(output_dir, out_name)
        sf.write(out_path, segment, target_sr)
        saved_files.append(out_path)

    return saved_files

def split_all_audios(input_root, output_root):
    os.makedirs(output_root, exist_ok=True)

    for label in os.listdir(input_root):
        label_path = os.path.join(input_root, label)
        
        if not os.path.isdir(label_path):
            continue
        
        output_label_dir = os.path.join(output_root, label)
        os.makedirs(output_label_dir, exist_ok=True)
        for fname in os.listdir(label_path):
            if not fname.lower().endswith(SUPPORTED_EXTENSIONS):
                continue
            
            file_path = os.path.join(label_path, fname)
            
            try:
                saved_files = split_audio_file(
                    file_path=file_path,
                    output_dir=output_label_dir,
                    target_sr=TARGET_SR,
                    segment_seconds=SEGMENT_SECONDS,
                    min_last_seconds=MIN_LAST_SECONDS
                )
                print(f"[완료] {label}/{fname} -> {len(saved_files)}개")
                
            except Exception as e:
                print(f"[에러] {label}/{fname}: {e}")

if __name__ == "__main__":
    split_all_audios(INPUT_ROOT, OUTPUT_ROOT)