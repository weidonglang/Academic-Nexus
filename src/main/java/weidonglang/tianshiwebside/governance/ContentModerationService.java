package weidonglang.tianshiwebside.governance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
public class ContentModerationService {
    private final JdbcTemplate jdbcTemplate;

    public ContentModerationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ModerationResult check(String scene, String content, String operator, boolean blockHighRisk) {
        String safeContent = content == null ? "" : content;
        List<SensitiveWordRow> words = jdbcTemplate.query("""
                        select id, word, category, risk_level, enabled, created_at, updated_at
                        from sensitive_word
                        where enabled = true
                        order by risk_level desc, word asc
                        """,
                (rs, rowNum) -> new SensitiveWordRow(
                        rs.getLong("id"),
                        rs.getString("word"),
                        rs.getString("category"),
                        rs.getString("risk_level"),
                        rs.getBoolean("enabled"),
                        rs.getObject("created_at", Instant.class),
                        rs.getObject("updated_at", Instant.class)
                )
        );
        List<SensitiveWordRow> matched = words.stream()
                .filter(word -> !word.word().isBlank() && safeContent.contains(word.word()))
                .toList();
        String riskLevel = matched.stream()
                .map(SensitiveWordRow::riskLevel)
                .max(Comparator.comparingInt(this::riskWeight))
                .orElse("LOW");
        String action = matched.isEmpty() ? "PASS" : ("HIGH".equals(riskLevel) && blockHighRisk ? "BLOCK" : "RECORD");
        String matchedWords = String.join(",", matched.stream().map(SensitiveWordRow::word).toList());
        jdbcTemplate.update("""
                        insert into content_moderation_log
                          (scene, content_hash, matched_words, risk_level, action, operator, trace_id, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                scene,
                sha256(safeContent),
                matchedWords,
                riskLevel,
                action,
                operator,
                TraceIdHolder.get(),
                Instant.now()
        );
        ModerationResult result = new ModerationResult(scene, matchedWords, riskLevel, action);
        if ("BLOCK".equals(action)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容命中高风险敏感词，已拦截");
        }
        return result;
    }

    private int riskWeight(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    public record SensitiveWordRow(
            Long id,
            String word,
            String category,
            String riskLevel,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ModerationResult(
            String scene,
            String matchedWords,
            String riskLevel,
            String action
    ) {
    }
}
