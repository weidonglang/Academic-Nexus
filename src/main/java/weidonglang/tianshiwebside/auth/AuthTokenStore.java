package weidonglang.tianshiwebside.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌存储组件。
 *
 * 正常情况下 access token 和 refresh token 会写入 Redis，并设置过期时间；
 * 如果 Redis 不可用，则使用本地内存 Map 兜底，保证演示环境不会因为 Redis 没启动而无法登录。
 */
@Component
public class AuthTokenStore {
    private final StringRedisTemplate redisTemplate;
    private final Map<String, FallbackToken> fallbackTokens = new ConcurrentHashMap<>();

    public AuthTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存访问令牌。
     *
     * Redis Key 形如 auth:access:{token}，用于后续接口请求时校验当前 token 属于哪个用户。
     */
    public void saveAccessToken(String token, String username, Duration ttl) {
        save("auth:access:" + token, token, username, ttl);
    }

    /**
     * 保存刷新令牌。
     *
     * Redis Key 形如 auth:refresh:{token}，用于后续扩展无感刷新登录状态。
     */
    public void saveRefreshToken(String token, String username, Duration ttl) {
        save("auth:refresh:" + token, token, username, ttl);
    }

    public Optional<String> findAccessTokenOwner(String token) {
        return find("auth:access:" + token, token);
    }

    public void revokeAccessToken(String token) {
        String key = "auth:access:" + token;
        fallbackTokens.remove(token);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // Local fallback may be active when Redis is not available.
        }
    }

    private void save(String redisKey, String fallbackKey, String username, Duration ttl) {
        fallbackTokens.put(fallbackKey, new FallbackToken(username, Instant.now().plus(ttl)));
        try {
            redisTemplate.opsForValue().set(redisKey, username, ttl);
        } catch (RuntimeException ignored) {
            // Keep local development usable without Redis.
        }
    }

    private Optional<String> find(String redisKey, String fallbackKey) {
        try {
            String username = redisTemplate.opsForValue().get(redisKey);
            if (username != null) {
                return Optional.of(username);
            }
        } catch (RuntimeException ignored) {
            // Fall through to in-memory store.
        }

        FallbackToken token = fallbackTokens.get(fallbackKey);
        if (token == null) {
            return Optional.empty();
        }
        if (token.expiresAt().isBefore(Instant.now())) {
            fallbackTokens.remove(fallbackKey);
            return Optional.empty();
        }
        return Optional.of(token.username());
    }

    private record FallbackToken(String username, Instant expiresAt) {
    }
}
