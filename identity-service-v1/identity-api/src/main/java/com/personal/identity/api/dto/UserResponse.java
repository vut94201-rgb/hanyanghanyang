package com.personal.identity.api.dto;

import com.personal.identity.core.role.Role;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response trả về sau register, hoặc {@code GET /me}.
 *
 * <p><b>Lưu ý đặc biệt:</b> KHÔNG có field {@code passwordHash}. Nếu thêm field
 * này (dù vô ý), 1 lần leak DTO ra log/response là leak hash cho toàn bộ DB.
 * Trách nhiệm bảo vệ {@code passwordHash} thuộc về tầng DTO này, KHÔNG phải core.
 *
 * <p>{@code roles} là set string {@code roleCode} cho gọn, không nest full Role
 * object. Nếu client cần permission detail, gọi endpoint riêng.
 */
public record UserResponse(
        Long id,
        String username,
        String emailAddress,
        String fullName,
        UserStatus accountStatus,
        Set<String> roles,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmailAddress(),
                user.getFullName(),
                user.getAccountStatus(),
                roleCodes,
                user.getCreatedAt()
        );
    }
}