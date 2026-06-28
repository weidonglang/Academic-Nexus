package weidonglang.tianshiwebside.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import weidonglang.tianshiwebside.auth.dto.LoginRequest;
import weidonglang.tianshiwebside.auth.dto.LoginResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.security.SecurityProperties;
import weidonglang.tianshiwebside.user.mapper.UserAccountMapper;
import weidonglang.tianshiwebside.user.UserStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final int MAX_LOGIN_FAILURES = 5;

    private final SecurityProperties securityProperties;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final AuthTokenStore tokenStore;

    public AuthService(
            SecurityProperties securityProperties,
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redisTemplate,
            AuthTokenStore tokenStore
    ) {
        this.securityProperties = securityProperties;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.tokenStore = tokenStore;
    }

    @Transactional
    /**
     * 功能：实现用户登录认证。
     * 说明：根据账号查询用户，校验账号状态和 BCrypt 密码，登录成功后生成访问 token、
     * 刷新 token，并返回用户角色；前端根据这些数据维护登录状态和菜单权限。
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        ensureNotLocked(username);

        UserAccountMapper.UserAccountRow user = userAccountMapper.findByUsername(username);
        if (user == null) {
            throw invalidCredentials(username);
        }

        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account is unavailable");
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials(username);
        }

        clearFailures(username);
        userAccountMapper.updateLastLoginAt(user.id(), Instant.now());
        var roles = userAccountMapper.findRoleCodesByUserId(user.id());

        return issueSession(user, roles);
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is required");
        }
        String token = refreshToken.trim();
        String username = tokenStore.findRefreshTokenOwner(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired"));
        UserAccountMapper.UserAccountRow user = userAccountMapper.findByUsername(username);
        if (user == null || user.status() != UserStatus.ACTIVE) {
            tokenStore.revokeRefreshToken(token);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Account is unavailable");
        }
        tokenStore.revokeRefreshToken(token);
        return issueSession(user, userAccountMapper.findRoleCodesByUserId(user.id()));
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            tokenStore.revokeAccessToken(accessToken.trim());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.revokeRefreshToken(refreshToken.trim());
        }
    }

    private LoginResponse issueSession(UserAccountMapper.UserAccountRow user, java.util.List<String> roles) {
        Instant expiresAt = Instant.now().plus(securityProperties.accessTokenTtl());
        String accessToken = "dev-access-" + UUID.randomUUID();
        String refreshToken = "dev-refresh-" + UUID.randomUUID();
        tokenStore.saveAccessToken(accessToken, user.username(), securityProperties.accessTokenTtl());
        tokenStore.saveRefreshToken(refreshToken, user.username(), securityProperties.refreshTokenTtl());

        return new LoginResponse(
                accessToken,
                refreshToken,
                expiresAt,
                new LoginResponse.UserSession(
                        user.id(),
                        user.username(),
                        user.displayName(),
                        roles
                )
        );
    }

    /**
     * 功能：处理账号或密码错误。
     * 说明：连续失败次数会记录到 Redis 的 auth:failures:{username}，
     * 达到阈值后临时锁定登录，防止简单暴力破解。
     */
    private BusinessException invalidCredentials(String username) {
        long failures = incrementFailures(username);
        if (failures >= MAX_LOGIN_FAILURES) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Too many login failures. Please try again in 10 minutes");
        }
        return new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
    }

    /**
     * 功能：检查账号是否因为连续登录失败被临时锁定。
     * 说明：从 Redis 读取失败次数，超过上限时拒绝本次登录请求。
     */
    private void ensureNotLocked(String username) {
        String value = readRedisValue(failureKey(username));
        if (value != null && Long.parseLong(value) >= MAX_LOGIN_FAILURES) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Too many login failures. Please try again later");
        }
    }

    private long incrementFailures(String username) {
        Long value = incrementRedisValue(failureKey(username));
        expireRedisValue(failureKey(username), Duration.ofMinutes(10));
        return value == null ? 1 : value;
    }

    /**
     * 功能：清理登录失败次数。
     * 说明：用户成功登录后删除 Redis 中的失败计数，避免历史失败影响后续正常登录。
     */
    private void clearFailures(String username) {
        try {
            redisTemplate.delete(failureKey(username));
        } catch (RuntimeException ignored) {
            // Redis is a guardrail for lockout, not the source of user truth.
        }
    }

    private String failureKey(String username) {
        // 登录失败计数 key，用于记录某账号短时间内连续输错密码的次数。
        return "auth:failures:" + username;
    }

    private String readRedisValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Long incrementRedisValue(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (RuntimeException ignored) {
            return 1L;
        }
    }

    private void expireRedisValue(String key, Duration ttl) {
        try {
            redisTemplate.expire(key, ttl);
        } catch (RuntimeException ignored) {
            // No-op in local fallback mode.
        }
    }
}
