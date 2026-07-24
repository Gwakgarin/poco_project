from datetime import datetime, timedelta
from enum import Enum

from ai.src.session.session_config import MEAL_PARAMS

COOKING_LABELS = {"cooking", "microwave"}


class MealState(Enum):
    IDLE = "IDLE"
    PREPARING = "PREPARING"
    CONFIRMED = "CONFIRMED"
    ENDED = "ENDED"


class MealSession:
    def __init__(self, params: dict = None):
        self.params = params or MEAL_PARAMS
        self.state = MealState.IDLE

        self.start_time: datetime | None = None      # PREPARING 시작 시각
        self.confirmed_time: datetime | None = None   # CONFIRMED(식사 확정) 시각
        self.last_event_time: datetime | None = None  # 마지막 관련 이벤트 시각
        self.end_time: datetime | None = None
        self.end_reason: str | None = None            # "timeout" | "dishwashing_trigger" | "no_activity"

        self.on_close = None  # 외부에서 세션 종료 시 콜백 등록 가능 (session: MealSession) -> None

    # 이벤트 처리
    def process_event(self, label: str, confidence: float, timestamp: datetime):
        """오디오 이벤트 하나를 받아 상태를 갱신한다."""

        # 진행 중인 세션은 먼저 timeout 여부부터 체크
        if self.state in (MealState.PREPARING, MealState.CONFIRMED):
            self._check_timeout(timestamp)
            if self.state == MealState.ENDED:
                # timeout으로 이미 종료됐으면 이번 이벤트로 새 세션을 열지 여부만 판단
                pass

        if self.state == MealState.IDLE:
            if label in COOKING_LABELS:
                self._start(timestamp)

        elif self.state == MealState.PREPARING:
            if label in COOKING_LABELS:
                self.last_event_time = timestamp
            elif label == "dishes":
                self._confirm(timestamp)

        elif self.state == MealState.CONFIRMED:
            if label in COOKING_LABELS or label == "dishes":
                self.last_event_time = timestamp

        # ENDED 상태에서 새로운 cooking 이벤트가 오면 새 세션 시작
        if self.state == MealState.ENDED and label in COOKING_LABELS:
            self._start(timestamp)

    def _start(self, timestamp: datetime):
        self.state = MealState.PREPARING
        self.start_time = timestamp
        self.last_event_time = timestamp
        self.confirmed_time = None
        self.end_time = None
        self.end_reason = None

    def _confirm(self, timestamp: datetime):
        self.state = MealState.CONFIRMED
        self.confirmed_time = timestamp
        self.last_event_time = timestamp

    def _check_timeout(self, current_time: datetime):
        if self.last_event_time is None:
            return

        if self.state == MealState.PREPARING:
            limit = timedelta(minutes=self.params["T1"])
            if current_time - self.last_event_time > limit:
                self._close(current_time, reason="no_activity")

        elif self.state == MealState.CONFIRMED:
            limit = timedelta(minutes=self.params["T2"])
            if current_time - self.last_event_time > limit:
                self._close(current_time, reason="timeout")

    # 설거지 세션 -> 식사 세션 종료 트리거 
    def trigger_end_by_dishwashing(self, timestamp: datetime):
        
        if self.state == MealState.CONFIRMED:
            self._close(timestamp, reason="dishwashing_trigger")

    def _close(self, timestamp: datetime, reason: str):
        was_confirmed = self.state == MealState.CONFIRMED
        self.state = MealState.ENDED
        self.end_time = timestamp
        self.end_reason = reason

        if was_confirmed and self.on_close:
            self.on_close(self)

    def is_active(self) -> bool:
        return self.state in (MealState.PREPARING, MealState.CONFIRMED)

    def is_confirmed(self) -> bool:
        return self.state == MealState.CONFIRMED

    def to_record(self) -> dict:
        return {
            "behavior": "meal",
            "start_time": self.start_time,
            "confirmed_time": self.confirmed_time,
            "end_time": self.end_time,
            "end_reason": self.end_reason,
        }
