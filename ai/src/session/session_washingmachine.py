from datetime import timedelta
from enum import Enum

from ai.src.session.session_config import LAUNDRY_PARAMS


class LaundryState(Enum):
    IDLE = "IDLE"
    ACTIVE = "ACTIVE"
    CONFIRMED = "CONFIRMED"
    ENDED = "ENDED"


class LaundrySession:
    def __init__(self, params=None):
        self.params = params or LAUNDRY_PARAMS
        self.state = LaundryState.IDLE

        self.start_time = None
        self.confirmed_time = None
        self.last_event_time = None
        self.end_time = None

        self.on_close = None

    def process_event(self, label, confidence, timestamp):
        # 진행 중인 세션은 현재 이벤트 처리 전에 timeout 확인
        if self.state in (LaundryState.ACTIVE, LaundryState.CONFIRMED):
            self._check_timeout(timestamp)

        if label != "washing_machine":
            return

        if self.state in (LaundryState.IDLE, LaundryState.ENDED):
            self._start(timestamp)

        elif self.state == LaundryState.ACTIVE:
            self.last_event_time = timestamp

            # 최초 감지 후 T2분 이상 지속되면 세탁 확정
            if timestamp - self.start_time >= timedelta(
                minutes=self.params["T2"]
            ):
                self.state = LaundryState.CONFIRMED
                self.confirmed_time = timestamp

        elif self.state == LaundryState.CONFIRMED:
            self.last_event_time = timestamp

    def _start(self, timestamp):
        self.state = LaundryState.ACTIVE
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

        confirmed = self.state == LaundryState.CONFIRMED

        self.end_time = self.last_event_time + limit
        self.state = LaundryState.ENDED

        # 확정된 세탁 세션만 기록 콜백 실행
        if confirmed and self.on_close:
            self.on_close(self)

    def check_timeout(self, current_time):
        # 새 이벤트가 없어도 외부 루프에서 종료 검사 가능
        if self.state in (LaundryState.ACTIVE, LaundryState.CONFIRMED):
            self._check_timeout(current_time)

    def is_active(self):
        return self.state in (
            LaundryState.ACTIVE,
            LaundryState.CONFIRMED,
        )

    def is_confirmed(self):
        return self.state == LaundryState.CONFIRMED

    def to_record(self):
        # 종료된 확정 세탁 세션만 기록
        if (
            self.state != LaundryState.ENDED
            or self.confirmed_time is None
        ):
            return None

        return {
            "behavior": "laundry",
            "start_time": self.start_time,
            "confirmed_time": self.confirmed_time,
            "end_time": self.end_time,
        }