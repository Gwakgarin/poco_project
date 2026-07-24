from datetime import datetime, timedelta
from enum import Enum

from ai.src.session.session_config import DISHWASHING_PARAMS
from ai.src.session.session_meal import MealSession


class DishwashingState(Enum):
    IDLE = "IDLE"
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    ENDED = "ENDED"


class DishwashingSession:
    def __init__(self, meal_session: MealSession = None, params: dict = None):
        self.params = params or DISHWASHING_PARAMS
        self.state = DishwashingState.IDLE

        self.start_time: datetime | None = None       # water 최초 감지 시각
        self.confirmed_time: datetime | None = None    # dishes로 확정된 시각
        self.last_event_time: datetime | None = None
        self.end_time: datetime | None = None
        self.branch: str | None = None                 # "meal_trigger" | "independent"

        self.last_microwave_time: datetime | None = None  # L분 lookback 판단용

        # 식사 세션 연동 (Step 4에서 트리거 호출)
        self.meal_session = meal_session
        self.on_close = None  # 외부 콜백: (session: DishwashingSession) -> None

    def process_event(self, label: str, confidence: float, timestamp: datetime):
        # microwave는 washing 상태와 무관하게 lookback 판단용으로만 기록
        if label == "microwave":
            self.last_microwave_time = timestamp

        # 진행 중인 세션은 먼저 timeout 여부 체크
        if self.state in (DishwashingState.PENDING, DishwashingState.CONFIRMED):
            self._check_timeout(timestamp)

        if self.state == DishwashingState.IDLE:
            if label == "water":
                self._start(timestamp)

        elif self.state == DishwashingState.PENDING:
            if label == "water":
                self.last_event_time = timestamp
            elif label == "dishes":
                self._confirm(timestamp)

        elif self.state == DishwashingState.CONFIRMED:
            if label in ("water", "dishes"):
                self.last_event_time = timestamp

        if self.state == DishwashingState.ENDED and label == "water":
            self._start(timestamp)

    def _start(self, timestamp: datetime):
        self.state = DishwashingState.PENDING
        self.start_time = timestamp
        self.last_event_time = timestamp
        self.confirmed_time = None
        self.end_time = None
        self.branch = None

    def _confirm(self, timestamp: datetime):
        self.state = DishwashingState.CONFIRMED
        self.confirmed_time = timestamp
        self.last_event_time = timestamp

    def _check_timeout(self, current_time: datetime):
        if self.last_event_time is None:
            return

        if self.state == DishwashingState.PENDING:
            limit = timedelta(minutes=self.params["T2"])
            if current_time - self.last_event_time > limit:
                # dishes 없이 폐기 (단순 손 씻기 등) -> 기록 없이 IDLE로
                self.state = DishwashingState.IDLE
                self.start_time = None

        elif self.state == DishwashingState.CONFIRMED:
            limit = timedelta(minutes=self.params["T3"])
            if current_time - self.last_event_time > limit:
                self._close(current_time)

    # Step 4: 후처리 분기
    def _close(self, timestamp: datetime):
        self.state = DishwashingState.ENDED
        self.end_time = timestamp

        lookback = timedelta(minutes=self.params["L"])
        has_recent_microwave = (
            self.last_microwave_time is not None
            and self.start_time is not None
            and self.start_time - self.last_microwave_time <= lookback
            and self.last_microwave_time <= self.start_time
        )

        if has_recent_microwave:
            self.branch = "meal_trigger"
            if self.meal_session is not None:
                self.meal_session.trigger_end_by_dishwashing(timestamp)
        else:
            self.branch = "independent"

        if self.on_close:
            self.on_close(self)

    def is_active(self) -> bool:
        return self.state in (DishwashingState.PENDING, DishwashingState.CONFIRMED)

    def to_record(self) -> dict | None:
        if self.branch != "independent":
            return None
        return {
            "behavior": "dishwashing",
            "start_time": self.start_time,
            "confirmed_time": self.confirmed_time,
            "end_time": self.end_time,
        }
