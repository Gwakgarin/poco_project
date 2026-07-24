from datetime import datetime, timedelta

from ai.src.session.session_meal import MealSession, MealState
from ai.src.session.session_dishwashing import DishwashingSession, DishwashingState
from ai.src.session.session_washingmachine import LaundrySession, LaundryState
from ai.src.session.session_cleaning import CleaningSession, CleaningState

T0 = datetime(2026, 1, 1, 12, 0, 0)


def minutes(n):
    return timedelta(minutes=n)


# ---------- meal ----------

def test_meal_confirms_on_cooking_then_dishes():
    session = MealSession()
    session.process_event("cooking", 0.9, T0)
    assert session.state == MealState.PREPARING

    session.process_event("dishes", 0.9, T0 + minutes(2))
    assert session.state == MealState.CONFIRMED
    assert session.is_confirmed()


def test_meal_closes_on_timeout_after_confirm():
    closed = []
    session = MealSession()
    session.on_close = lambda s: closed.append(s.to_record())

    session.process_event("cooking", 0.9, T0)
    session.process_event("dishes", 0.9, T0 + minutes(2))
    # T2 = 20min idle limit after CONFIRMED — push past it with any event
    session.process_event("tv", 0.5, T0 + minutes(23))

    assert session.state == MealState.ENDED
    assert session.end_reason == "timeout"
    assert len(closed) == 1
    assert closed[0]["behavior"] == "meal"


def test_meal_ends_early_on_dishwashing_trigger():
    closed = []
    session = MealSession()
    session.on_close = lambda s: closed.append(s)

    session.process_event("cooking", 0.9, T0)
    session.process_event("dishes", 0.9, T0 + minutes(2))
    assert session.is_confirmed()

    session.trigger_end_by_dishwashing(T0 + minutes(5))

    assert session.state == MealState.ENDED
    assert session.end_reason == "dishwashing_trigger"
    assert len(closed) == 1


def test_meal_without_dishes_times_out_unconfirmed():
    closed = []
    session = MealSession()
    session.on_close = lambda s: closed.append(s)

    session.process_event("cooking", 0.9, T0)
    # T1 = 10min limit while PREPARING (no "dishes" ever arrives)
    session.process_event("tv", 0.5, T0 + minutes(15))

    assert session.state == MealState.ENDED
    assert session.end_reason == "no_activity"
    assert not session.is_confirmed()
    # on_close only fires for a *confirmed* session
    assert closed == []
    # to_record() must refuse to hand back an unconfirmed session
    assert session.to_record() is None


# ---------- dishwashing ----------

def test_dishwashing_confirms_and_closes_independent():
    closed = []
    session = DishwashingSession()
    session.on_close = lambda s: closed.append(s.to_record())

    session.process_event("water", 0.9, T0)
    assert session.state == DishwashingState.PENDING

    session.process_event("dishes", 0.9, T0 + minutes(1))
    assert session.state == DishwashingState.CONFIRMED

    # T3 = 5min idle limit after CONFIRMED, no recent microwave -> independent
    session.process_event("tv", 0.5, T0 + minutes(7))

    assert session.state == DishwashingState.ENDED
    assert session.branch == "independent"
    assert len(closed) == 1
    assert closed[0]["behavior"] == "dishwashing"


def test_dishwashing_meal_trigger_branch_closes_meal_session():
    meal = MealSession()
    meal.process_event("cooking", 0.9, T0)
    meal.process_event("dishes", 0.9, T0 + minutes(2))
    assert meal.is_confirmed()

    dish_closed = []
    dish = DishwashingSession(meal_session=meal)
    dish.on_close = lambda s: dish_closed.append(s)

    # microwave lookback (L=60min) then water/dishes shortly after
    dish.process_event("microwave", 0.9, T0)
    dish.process_event("water", 0.9, T0 + minutes(10))
    dish.process_event("dishes", 0.9, T0 + minutes(11))
    dish.process_event("tv", 0.5, T0 + minutes(20))  # past T3=5min -> closes

    assert dish.state == DishwashingState.ENDED
    assert dish.branch == "meal_trigger"
    assert dish.to_record() is None  # not persisted standalone
    assert meal.state == MealState.ENDED
    assert meal.end_reason == "dishwashing_trigger"


def test_dishwashing_pending_without_dishes_discards_silently():
    closed = []
    session = DishwashingSession()
    session.on_close = lambda s: closed.append(s)

    session.process_event("water", 0.9, T0)
    # T2 = 2min limit while PENDING, no "dishes" ever arrives
    session.process_event("tv", 0.5, T0 + minutes(5))

    assert session.state == DishwashingState.IDLE
    assert session.start_time is None
    assert closed == []


# ---------- laundry ----------

def test_laundry_confirms_after_sustained_detection_and_closes():
    closed = []
    session = LaundrySession()
    session.on_close = lambda s: closed.append(s.to_record())

    # gaps <= T1=5min so it never times out mid-cycle; total span reaches T2=20min
    for offset in (0, 5, 10, 15, 20):
        session.process_event("washing_machine", 0.9, T0 + minutes(offset))

    assert session.state == LaundryState.CONFIRMED
    assert session.is_confirmed()

    # external timeout check after T1=5min of silence
    session.check_timeout(T0 + minutes(26))

    assert session.state == LaundryState.ENDED
    assert len(closed) == 1
    assert closed[0]["behavior"] == "laundry"


def test_laundry_short_burst_times_out_unconfirmed_and_not_recorded():
    closed = []
    session = LaundrySession()
    session.on_close = lambda s: closed.append(s)

    session.process_event("washing_machine", 0.9, T0)
    session.check_timeout(T0 + minutes(6))  # > T1=5min, never reached T2=20min

    assert session.state == LaundryState.ENDED
    assert not session.is_confirmed()
    assert session.to_record() is None
    assert closed == []


# ---------- cleaning ----------

def test_cleaning_confirms_after_sustained_detection_and_closes():
    closed = []
    session = CleaningSession()
    session.on_close = lambda s: closed.append(s.to_record())

    # gaps <= T1=5min; total span reaches T2=5min
    for offset in (0, 3, 6):
        session.process_event("vacuum", 0.9, T0 + minutes(offset))

    assert session.state == CleaningState.CONFIRMED

    session.check_timeout(T0 + minutes(12))  # > T1=5min after last event

    assert session.state == CleaningState.ENDED
    assert len(closed) == 1
    assert closed[0]["behavior"] == "cleaning"


def test_cleaning_short_burst_times_out_unconfirmed_and_not_recorded():
    closed = []
    session = CleaningSession()
    session.on_close = lambda s: closed.append(s)

    session.process_event("vacuum", 0.9, T0)
    session.check_timeout(T0 + minutes(6))  # > T1=5min, never reached T2=5min

    assert session.state == CleaningState.ENDED
    assert not session.is_confirmed()
    assert session.to_record() is None
    assert closed == []
