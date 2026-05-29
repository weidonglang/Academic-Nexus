package weidonglang.tianshiwebside.auth.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        UserSession user
) {
    public record UserSession(
            Long id,
            String username,
            String displayName,
            List<String> roles
    ) {
    }
}
