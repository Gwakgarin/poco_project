# POCO

**소리로 지키는 가족의 하루 — 경도인지장애(MCI) 대상자를 위한 온디바이스 AI 돌봄 서비스**

POCO는 스마트폰 마이크로 주변 소리를 실시간으로 분석해 위험 상황(비명, 반복된 경적 등)을 감지하고, 세탁·식사·설거지·청소 같은 생활 패턴을 자동으로 파악해 보호자에게 전달하는 Android 앱입니다. 카메라 없이 소리와 위치 정보만 사용하므로 사생활을 침해하지 않으면서, 혼자 지내는 경도인지장애 대상자의 일상 속 이상 징후를 조기에 알아챌 수 있습니다.

## 배경

카메라 기반 모니터링은 사생활 노출에 대한 부담이 크고, 웨어러블 방식은 대상자가 항상 착용해야 한다는 제약이 있습니다. POCO는 별도 기기 없이 이미 집에 있는 스마트폰 한 대로, 소리와 위치만 활용해 위험 신호와 생활 리듬의 변화를 파악하는 방식을 선택했습니다.

## 화면

**사용자 모드** (돌봄 대상자의 폰)

| 홈 | 분석 | 큰 소리 확인 |
| :---: | :---: | :---: |
| <img src="docs/images/user_home.png" width="220" alt="사용자 홈"> | <img src="docs/images/user_analysis.png" width="220" alt="사용자 분석"> | <img src="docs/images/user_sos.png" width="220" alt="큰 소리 확인"> |
| 실시간 소리 감지 상태, 재실 여부, 배터리 | 오늘 감지된 활동 기록 | 평소보다 큰 소리 감지 시 확인, 응답이 없으면 보호자에게 알림 |

**보호자 모드** (보호자의 폰)

| 홈 | 타임라인 | 추이 | 알림센터 |
| :---: | :---: | :---: | :---: |
| <img src="docs/images/guardian_home.png" width="190" alt="보호자 홈"> | <img src="docs/images/guardian_timeline.png" width="190" alt="타임라인"> | <img src="docs/images/guardian_trend.png" width="190" alt="장기 추이"> | <img src="docs/images/guardian_alerts.png" width="190" alt="알림센터"> |
| 오늘의 활동 요약, 실시간 상태, 최근 위험 알림 | 오늘의 행동 기록 | 1주·1개월·6개월 활동 추이와 생활 규칙성 | 위험 알림, 이상 탐지 알림 |

## 주요 기능

**사용자 모드**
- **실시간 소리 인식**: 마이크 입력을 온디바이스에서 분류하고, 큰 소리를 분석하는 동안에는 "소리 분석 중"으로 표시합니다.
- **생활 패턴 기록**: 세탁, 식사, 설거지, 청소, 대화·TV 같은 소리를 활동으로 묶어 "오늘의 활동 기록"으로 보여주고, 폰의 움직임으로 취침·기상 시각도 추정합니다.
- **재실 판단**: GPS로 집 안(HOME)과 집 밖(OUTSIDE)을 구분합니다. 집 위치는 "집으로 설정"으로 지정합니다.
- **SOS**: 위급할 때 SOS를 요청하면 서버에 긴급 요청이 기록되고 보호자에게 알림이 전달됩니다. 119 등 외부 기관과의 실제 연동은 되어 있지 않습니다.
- **설정**: 마이크 감도, 알림 설정, 보호자 연동 관리(QR 초대), 계정 정보를 제공합니다.

**보호자 모드**
- **홈**: 식사 횟수, 외출 여부, 인지 활동 시간, 수면 시간, 마이크·GPS·배터리 상태와 오늘의 일간 타임라인을 한눈에 보여줍니다. 외출 중에는 "외출 중"으로 표시됩니다.
- **타임라인**: 오늘 감지된 행동 기록을 시간 순서대로 제공합니다.
- **추이**: 1주, 1개월, 6개월 단위로 외출 횟수와 가사 활동량, 하루 평균 식사 기록, 식사 시간 규칙성, 평균 수면 시간을 보여줍니다.
- **알림센터**: 비명·반복 경적 같은 위험 알림과 활동 변화를 알리는 이상 탐지 알림(외출 빈도 급감, 인지활동 감소, 식사 불규칙)을 모아 보여줍니다. 위험 알림은 푸시로도 전달됩니다.
- **긴급 대응**: 위치 확인(지도)과 전화 연결 화면을 제공합니다.
- **설정**: 사용자 연동 정보, 알림 수신 설정, 새 사용자 연동하기(QR 스캔)를 제공합니다.

**공통**
- 이메일 기반 회원가입과 로그인, 역할(사용자/보호자) 선택, 기기 자동 등록
- QR 코드 기반 보호자 연동과 관계 지정(가족/요양보호사/친구/기타), 다중 보호자 지원
- 사용자 폰의 배터리 잔량이 바뀔 때마다 서버에 올라가 보호자 화면에 표시됩니다.

## 소리 인식과 판정 방식

개별 소리 이벤트를 규칙 엔진이 활동 단위로 묶어 기록합니다.

| 대상 | 쓰는 소리 | 판정 규칙 |
| --- | --- | --- |
| 비명 | scream | 감지 즉시 위험으로 판단하고 보호자에게 알림 (위치 무관) |
| 반복 경적 | car_horn | **집 밖**에서 30초 안에 3회 이상 감지되면 위험 후보. 집 안이거나 위치가 불확실하면 판정에 쓰지 않음 |
| 세탁 | 세탁기 | 20분 이상 지속되면 세탁으로 기록 (소리가 끝나고 5분 뒤 확정) |
| 청소 | 청소기 | 2분 이상 지속되면 청소로 기록 |
| 인지 활동 | 대화, TV | 5분 이상 지속되면 기록 |
| 식사 | 전자레인지, 조리 소리 → 그릇 소리 | 조리 뒤 그릇 소리가 이어지면 식사로 확정. 이어지는 설거지는 식사에 합쳐짐 |
| 설거지 | 물소리 → 그릇 소리 | 직전 60분 안에 전자레인지 소리가 없을 때만 단독 설거지로 기록 |
| 수면 | 폰 움직임, 소리 없음 | 30분 동안 활동이 없으면 취침, 움직임이 5분 이어지면 기상 |

이 규칙은 온디바이스 Kotlin 코드(`session/*`)와 오프라인 검증용 Python 코드(`ai/src/session/`)로 나뉘어 있고, 실제 앱 동작은 Kotlin 코드를 따릅니다.

## 시스템 구성

```mermaid
flowchart LR
    subgraph Android["Android 앱"]
        A["마이크 수집\n(AudioMonitorService)"] --> B["위험 판정\n(DangerPolicy)"]
        L["위치 추적\n(LocationTracker)"] --> B
        B --> UI["Compose UI\n(사용자/보호자)"]
    end

    subgraph AI["AI 파이프라인 (온디바이스)"]
        E["YAMNet 임베딩 추출"] --> C["TFLite 분류기\n(PocoClassifier)"]
        C --> S["행동 세션 규칙 엔진\n(session/*)"]
    end

    subgraph Backend["백엔드 (backend 브랜치, Spring Boot)"]
        API["REST API\n회원가입·로그인, 기기 등록,\n보호자 연동, 알림 설정, 응급 요청,\n소리·행동·위치 이벤트 저장"]
        DB[(MySQL poco_db)]
        API --> DB
    end

    Android <-->|Retrofit/OkHttp\nServerApi| API
    AI --> Android
```

AI 모델(TFLite)은 앱에 내장되어 온디바이스로 1차 분류를 수행하며, 분류·세션 결과와 위치·긴급 알림은 REST API를 통해 서버와 동기화됩니다. 백엔드는 이 저장소의 **`backend` 브랜치**에 별도 Spring Boot 프로젝트로 존재합니다(자세한 내용은 [브랜치 구성](#브랜치-구성) 참고). 데이터 모델은 `backend` 브랜치의 [`entity/`](https://github.com/Gwakgarin/poco_project/tree/backend/src/main/java/com/project/poco/entity) 패키지가 최신 기준입니다.

## 브랜치 구성

| 브랜치 | 내용 |
| --- | --- |
| `main` | Android 앱(Kotlin/Compose)과 AI 학습 파이프라인. 이 README가 다루는 기본 브랜치 |
| `backend` | Spring Boot 기반 REST API 서버 (Java 21, Spring Data JPA, MySQL). `main`의 `ServerApi.kt`가 호출하는 API를 구현 |

두 브랜치는 서로 다른 런타임(Android/JVM 서버)이라 병합 없이 독립적으로 유지되며, API 계약은 `main`의 [`ServerApi.kt`](android/app/src/main/java/com/example/poco/ServerApi.kt)와 `backend`의 컨트롤러/엔티티 코드로 맞춰갑니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Navigation Compose, Material3 |
| 온디바이스 추론 | TensorFlow Lite (LiteRT), YAMNet 임베딩 |
| 네트워킹 | Retrofit2, Gson, OkHttp |
| 위치 | Google Play Services Location |
| AI 모델 학습 | TensorFlow, TensorFlow Hub, scikit-learn, librosa, pandas |
| 백엔드 (`backend` 브랜치) | Java 21, Spring Boot 3, Spring Data JPA, MySQL, Lombok |

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
│       ├── session/                 # 소리 이벤트 → 행동 세션 (세탁/식사/설거지/청소/인지활동)
│       ├── location/                # 재실 판단(HOME/OUTSIDE), 위치 업로드
│       ├── push/                    # FCM 푸시 알림
│       └── ui/                      # 화면(Compose), 공통 컴포넌트, 테마
├── ai/                       # 오프라인 모델 학습·평가 파이프라인
│   ├── src/                  # 데이터셋 구축, 증강, 학습, TFLite 변환 스크립트
│   ├── src/session/          # 행동 세션 규칙의 Python 구현과 검증
│   ├── models/               # 학습된 분류기(.pkl)
│   └── data/                 # 학습/평가용 데이터셋
└── docs/images/              # README 화면 이미지
```

## 시작하기

### 요구 사항

| 구성 요소 | 버전 |
| --- | --- |
| Android | compileSdk 36, minSdk 24, targetSdk 36 (Android Studio 최신 버전 권장) |
| 백엔드(`backend` 브랜치) | JDK 21, MySQL 8.x |

### Android 앱 실행

```bash
cd android
./gradlew assembleDebug
```

Android Studio에서 `android/` 디렉터리를 열어 실행할 수도 있습니다. 실행 시 마이크(`RECORD_AUDIO`)와 위치 권한이 필요합니다.

보호자 위치 확인 화면의 Google 지도를 띄우려면 `android/local.properties`에 Maps API 키를 넣어야 합니다 (이 파일은 깃에 올라가지 않습니다). 키가 없어도 빌드·실행은 되며 지도 영역만 회색으로 표시됩니다.

```properties
MAPS_API_KEY=여기에_발급받은_키
```

Google Cloud 콘솔에서는 **Maps SDK for Android**만 사용 설정하고, 키의 API 제한도 그 하나로 걸어 두세요. 지도 표시는 무료이며, 길찾기·장소 검색 같은 과금 API는 앱에서 호출하지 않습니다.

### AI 파이프라인 (모델 학습/재현)

```bash
cd ai
pip install -r requirements.txt
python src/build_dataset.py
python src/train_classifier.py
python src/convert_to_tflite.py
```

### 백엔드 서버 실행 (`backend` 브랜치)

실서버는 이미 AWS EC2(`3.25.227.173:8080`)에 배포되어 상시 운영 중이며, 안드로이드 앱은 기본적으로 이 주소에 붙습니다. 별도로 로컬 백엔드를 띄울 필요 없이 바로 앱을 실행하면 됩니다.

<details>
<summary>백엔드 코드를 직접 수정·테스트하고 싶을 때 (로컬 실행, 참고용)</summary>

`backend`는 `main`과 공유 파일이 없는 완전히 별개의 Spring Boot 프로젝트입니다. 같은 작업 폴더에서 그냥 `git checkout backend`를 하면 `android/`, `ai/` 디렉터리가 작업 트리에서 통째로 사라져 보이니(히스토리엔 남아있지만 혼란스러움), **별도 폴더에 `git worktree`로 체크아웃하는 것을 권장합니다.**

```bash
git worktree add ../poco-backend backend
cd ../poco-backend

# MySQL에 poco_db 데이터베이스를 만든 뒤, application.properties가 참조하는
# 환경변수를 실제 값으로 설정합니다 (예시는 src/main/resources/application-example.properties 참고)
export DB_URL="jdbc:mysql://localhost:3306/poco_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
export DB_USERNAME="본인의 MySQL 계정"
export DB_PASSWORD="본인의 MySQL 비밀번호"

./gradlew bootRun
```

Android 앱의 `ServerApiClient.BASE_URL`(`android/app/src/main/java/com/example/poco/ServerApi.kt`)은 기본적으로 `http://3.25.227.173:8080/`(AWS EC2에 배포된 실제 서버)를 가리킵니다. 로컬 백엔드로 다시 테스트하려면 이 값을 아래 중 하나로 바꿔주세요.

- 에뮬레이터에서 로컬 백엔드에 붙일 때: `BASE_URL`을 `"http://10.0.2.2:8080/"`으로 변경
- USB + `adb reverse tcp:8080 tcp:8080`으로 실기기 연결할 때: `"http://127.0.0.1:8080/"`으로 변경

</details>

## 테스트

```bash
cd android
./gradlew test
```

- `DangerPolicyTest`(4개): scream/car_horn 등 환경음 종류, HOME/OUTSIDE 위치 상태, 반복 횟수·경과 시간에 따른 위험 판단 정책 검증
- `HomeStateMachineTest`(4개): 위치 정확도·좌표에 따른 HOME/OUTSIDE 상태 전환 및 불확실한 값 처리 검증
- `TrendAggregatorTest`(42개), `TrendUiStateMapperTest`(11개): 추이 화면의 기간별 집계, 변화율, 규칙성 계산과 UI 상태 변환 검증
- 위 61개 외에 Android 프로젝트 생성 시 기본 포함되는 보일러플레이트 테스트(`ExampleUnitTest`)가 있으나 프로젝트 로직 검증과는 무관해 제외했습니다.
- `backend` 브랜치에는 Spring Boot 기본 생성 시 포함되는 컨텍스트 로딩 테스트(`PocoApplicationTests`) 외에 별도 로직 테스트가 아직 없습니다.
