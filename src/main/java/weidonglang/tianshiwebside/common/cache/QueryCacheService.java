package weidonglang.tianshiwebside.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class QueryCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public QueryCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public <T> T get(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            // Redis query cache is optional. Database remains the source of truth.
        }

        T value = loader.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | java.io.IOException ignored) {
            // Keep reads working when Redis is unavailable or serialization fails.
        }
        return value;
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // Cache eviction must not break the write path.
        }
    }

    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ignored) {
            // Short TTLs bound staleness if Redis is unavailable during eviction.
        }
    }
}
