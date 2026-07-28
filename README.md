# POCO

**소리로 독거 어르신의 일상을 지키는 온디바이스 AI 안전 모니터링 앱**

POCO는 스마트폰 마이크로 주변 소리를 실시간으로 분석해 위험 상황(비명, 반복된 경적 등)을 감지하고, 세탁·설거지·식사·청소 같은 생활 패턴을 자동으로 인지해 보호자에게 전달하는 Android 앱입니다. 카메라 없이 소리와 위치 정보만으로 사생활을 침해하지 않으면서 일상의 이상 징후를 파악하는 것을 목표로 합니다.

## 배경

혼자 사는 어르신을 위한 기존 모니터링 솔루션은 카메라 기반이거나 웨어러블 착용을 전제로 하는 경우가 많아 거부감이 크고 실제 사용률이 낮습니다. POCO는 이미 집에 있는 스마트폰 하나로, 별도 기기 없이 소리 이벤트 분류만으로 위험과 생활 패턴을 추론하는 방식을 택했습니다.

## 주요 기능

- **실시간 위험 감지**: 마이크로 수집한 오디오를 온디바이스에서 분류해 비명(scream), 반복 경적(car_horn) 등 위험 신호를 즉시 판단하고 보호자에게 알림 전송
- **생활 패턴(행동 세션) 추론**: 개별 소리 이벤트를 세탁/설거지/식사/청소 등 의미 있는 활동 세션으로 묶어 타임라인·추이(trend)로 제공
- **위치 기반 재실 판단(HOME/OUTSIDE)**: GPS로 재실 상태를 추적해 위험 판단 정책(예: 경적은 외출 중일 때만 후보로 판단)에 반영
- **보호자-피보호자 계정 연동**: QR 코드 기반 링크 코드로 보호자와 피보호자 계정을 연결, 다중 보호자 지원
- **보호자 앱**: 홈 요약, 일일 모니터링, 활동 로그, 추이 그래프, 알림 센터, 긴급 대응(신고/연락) 화면 제공
- **알림/마이크 감도 설정**: 이상 감지 알림 on/off, 마이크 민감도 조정 등 사용자별 커스터마이징

## 시스템 구성

```
[Android 앱]                         [AI 파이프라인]                    [백엔드]
마이크 수집 (AudioMonitorService)  →  YAMNet 임베딩 추출                
  → 위치 추적 (LocationTracker)       → TFLite 분류기 (PocoClassifier)  
  → 위험 판정 (DangerPolicy)          → 행동 세션 규칙 엔진 (session/*)  → REST API (ServerApi/Retrofit)
  → Compose UI (User/Guardian)                                          → 소리·행동·위치 이벤트 저장, 알림
```

AI 모델(TFLite)은 앱에 내장되어 온디바이스로 1차 분류를 수행하며, 분류/세션 결과와 위치·긴급 알림은 REST API를 통해 서버와 동기화됩니다. 백엔드 API 스펙과 데이터 모델은 [`docs/POCO_ERD.md`](docs/POCO_ERD.md)에 정리되어 있습니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Navigation Compose, Material3 |
| 온디바이스 추론 | TensorFlow Lite (LiteRT), YAMNet 임베딩 |
| 네트워킹 | Retrofit2, Gson, OkHttp |
| 위치 | Google Play Services Location |
| AI 모델 학습 | TensorFlow, TensorFlow Hub, scikit-learn, librosa, pandas |

## 폴더 구조

```
poco_project/
├── android/                 # Android 앱 (Kotlin, Jetpack Compose)
│   └── app/src/main/java/com/example/poco/
│       ├── AudioMonitorService.kt   # 포그라운드 오디오 모니터링 서비스
│       ├── PocoClassifier.kt        # TFLite 소리 분류기
│       ├── YamNetEmbedder.kt        # YAMNet 임베딩 추출
│       ├── DangerPolicy.kt          # 소리+위치 조합 위험 판정 정책
│       ├── SleepDetector.kt         # 수면 상태 추론
│       ├── ServerApi.kt             # 백엔드 REST API 클라이언트
│       ├── location/                # 재실 판단(HOME/OUTSIDE), 위치 업로드
│       └── ui/screens/              # 사용자·보호자 화면 (Compose)
├── ai/                       # 오프라인 모델 학습·평가 파이프라인
│   ├── src/                  # 데이터셋 구축, 증강, 학습, TFLite 변환 스크립트
│   ├── src/session/          # 소리 이벤트 → 행동 세션 변환 규칙 (세탁/설거지/식사/청소)
│   ├── models/                # 학습된 분류기(.pkl)
│   └── data/                  # 학습/평가용 데이터셋
└── docs/
    └── POCO_ERD.md            # 백엔드 API 스펙 기반 ERD 및 설계 원칙
```

## 시작하기

### Android 앱 실행

```bash
cd android
./gradlew assembleDebug
```

Android Studio에서 `android/` 디렉터리를 열어 실행할 수도 있습니다. 실행 시 마이크(`RECORD_AUDIO`)와 위치 권한이 필요합니다.

### AI 파이프라인 (모델 학습/재현)

```bash
cd ai
pip install -r requirements.txt
python src/build_dataset.py
python src/train_classifier.py
python src/convert_to_tflite.py
```

## 문서

- [POCO ERD / 백엔드 API 스펙](docs/POCO_ERD.md)
