# 설거지 세션 (water + dishes, microwave 후처리 분기)
DISHWASHING_PARAMS = {
    "T2": 2,    # water -> dishes 최대 허용 gap (분)
    "T3": 5,    # 재감지 허용 gap / 세션 종료 timeout (분)
    "L": 60,    # microwave 선행 확인 lookback (분)
}

# 식사 세션 (cooking/microwave -> dishes, 설거지 트리거로 종료)
MEAL_PARAMS = {
    "T1": 10,   # 조리(microwave) 재감지 허용 gap (분)
    "T2": 20,   # CONFIRMED 이후 무감지 timeout (분) - 설거지 트리거보다 먼저 도달하면 이걸로 종료
}


# 세탁 세션
LAUNDRY_PARAMS = {
    "T1": 5,    # 세탁 재감지 gap / 종료 timeout
    "T2": 20,   # 세탁 최소 지속시간
}

# 청소 세션
CLEANING_PARAMS = {
    "T1": 5,    # 청소 재감지 gap / 종료 timeout (분)
    "T2": 5,    # 청소 최소 지속시간 (분)
}