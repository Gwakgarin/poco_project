package com.project.poco.entity;

/**
 * users.role 값 정의. ERD 확정안에 role 이 int 타입으로 되어 있어서
 * 기존 enum(Role) 대신 int 상수로 변경했습니다.
 * 0 = 피보호자(USER), 1 = 보호자(GUARDIAN)
 */
public final class RoleType {
    public static final int USER = 0;
    public static final int GUARDIAN = 1;

    private RoleType() {
    }
}
