from datetime import timedelta
from enum import Enum

from ai.src.session.session_config import CLEANING_PARAMS


class CleaningState(Enum):
    IDLE = "IDLE"
    ACTIVE = "ACTIVE"
    CONFIRMED = "CONFIRMED"
    ENDED = "ENDED"


class CleaningSession:
    def __init__(self, params=None):
        self.params = params or CLEANING_PARAMS
        self.state = CleaningState.IDLE

        self.start_time = None
        self.confirmed_time = None
        self.last_event_time = None
        self.end_time = None

        self.on_close = None

    def process_event(self, label, confidence, timestamp):
        # 진행 중인 세션은 현재 이벤트 처리 전에 timeout 확인
        if self.state in (CleaningState.ACTIVE, CleaningState.CONFIRMED):
            self._check_timeout(timestamp)

        if label != "vacuum":
            return

        if self.state in (CleaningState.IDLE, CleaningState.ENDED):
            self._start(timestamp)

        elif self.state == CleaningState.ACTIVE:
            self.last_event_time = timestamp

            # 최초 감지 후 T2분 이상 지속되면 청소 확정
            if timestamp - self.start_time >= timedelta(
                minutes=self.params["T2"]
            ):
                self.state = CleaningState.CONFIRMED
                self.confirmed_time = timestamp

        elif self.state == CleaningState.CONFIRMED:
            self.last_event_time = timestamp

    def _start(self, timestamp):
        self.state = CleaningState.ACTIVE
        self.start_time = timestamp
        self.last_event_time = timestamp

        self.confirmed_time = None
        self.end_time = None

    def _check_timeout(self, current_time):
        if self.last_event_time is None:
            return

        # 마지막 감지 후 T1분 초과 무감지 시 종료
        limit = timedelta(minutes=self.params["T1"])

        if current_time - self.last_event_time <= limit:
            return

        confirmed = self.state == CleaningState.CONFIRMED

        self.end_time = self.last_event_time + limit
        self.state = CleaningState.ENDED

        # 확정된 청소 세션만 기록
        if confirmed and self.on_close:
            self.on_close(self)

    def check_timeout(self, current_time):
        if self.state in (CleaningState.ACTIVE, CleaningState.CONFIRMED):
            self._check_timeout(current_time)

    def is_active(self):
        return self.state in (
            CleaningState.ACTIVE,
            CleaningState.CONFIRMED,
        )

    def is_confirmed(self):
        return self.state == CleaningState.CONFIRMED

    def to_record(self):
        if (
            self.state != CleaningState.ENDED
            or self.confirmed_time is None
        ):
            return None

        return {
            "behavior": "cleaning",
            "start_time": self.start_time,
            "confirmed_time": self.confirmed_time,
            "end_time": self.end_time,
        }