package weidonglang.tianshiwebside.user.mapper;

import weidonglang.tianshiwebside.user.UserStatus;

import java.time.Instant;

public record AdminUserRow(
        Long userId,
        String username,
        String displayName,
        UserStatus status,
        Instant lastLoginAt
) {
}
