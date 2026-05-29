package weidonglang.tianshiwebside.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
