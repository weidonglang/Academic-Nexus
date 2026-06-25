package weidonglang.tianshiwebside.governance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.api.PageResponse;
import weidonglang.tianshiwebside.common.api.Pagination;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
public class GovernanceController {
    private final JdbcTemplate jdbcTemplate;
    private final ContentModerationService moderationService;
    private final AuditLogService auditLogService;

    public GovernanceController(
            JdbcTemplate jdbcTemplate,
            ContentModerationService moderationService,
            AuditLogService auditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.moderationService = moderationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/admin/sensitive-words")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ContentModerationService.SensitiveWordRow>> sensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        List<ContentModerationService.SensitiveWordRow> records = jdbcTemplate.query("""
                        select id, word, category, risk_level, enabled, created_at, updated_at
                        from sensitive_word
                        order by updated_at desc, id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new ContentModerationService.SensitiveWordRow(
                        rs.getLong("id"),
                        rs.getString("word"),
                        rs.getString("category"),
                        rs.getString("risk_level"),
                        rs.getBoolean("enabled"),
                        rs.getObject("created_at", Instant.class),
                        rs.getObject("updated_at", Instant.class)
                ),
                safeSize,
                Pagination.offset(safePage, safeSize)
        );
        Long total = jdbcTemplate.queryForObject("select count(*) from sensitive_word", Long.class);
        return ApiResponse.success(new PageResponse<>(records, safePage, safeSize, total == null ? 0 : total));
    }

    @PostMapping("/api/admin/sensitive-words")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ContentModerationService.SensitiveWordRow> createSensitiveWord(
            Principal principal,
            @Valid @RequestBody SensitiveWordRequest request
    ) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                        insert into sensitive_word (word, category, risk_level, enabled, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                request.word().trim(),
                request.category().trim(),
                request.riskLevel(),
                request.enabled(),
                now,
                now
        );
        auditLogService.record(principal.getName(), "CREATE_SENSITIVE_WORD", "SENSITIVE_WORD",
                request.word(), request.category() + ":" + request.riskLevel(), TraceIdHolder.get());
        return ApiResponse.success(jdbcTemplate.queryForObject("""
                        select id, word, category, risk_level, enabled, created_at, updated_at
                        from sensitive_word
                        where word = ?
                        """,
                (rs, rowNum) -> new ContentModerationService.SensitiveWordRow(
                        rs.getLong("id"),
                        rs.getString("word"),
                        rs.getString("category"),
                        rs.getString("risk_level"),
                        rs.getBoolean("enabled"),
                        rs.getObject("created_at", Instant.class),
                        rs.getObject("updated_at", Instant.class)
                ),
                request.word().trim()
        ));
    }

    @PostMapping("/api/content-moderation/check")
    public ApiResponse<ContentModerationService.ModerationResult> checkContent(
            Principal principal,
            @Valid @RequestBody ModerationCheckRequest request
    ) {
        String operator = principal == null ? "anonymous" : principal.getName();
        return ApiResponse.success(moderationService.check(request.scene(), request.content(), operator, request.blockHighRisk()));
    }

    @GetMapping("/api/admin/content-moderation/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ModerationLogRow>> moderationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        List<ModerationLogRow> records = jdbcTemplate.query("""
                        select id, scene, content_hash, matched_words, risk_level, action, operator, trace_id, created_at
                        from content_moderation_log
                        order by created_at desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new ModerationLogRow(
                        rs.getLong("id"),
                        rs.getString("scene"),
                        rs.getString("content_hash"),
                        rs.getString("matched_words"),
                        rs.getString("risk_level"),
                        rs.getString("action"),
                        rs.getString("operator"),
                        rs.getString("trace_id"),
                        rs.getObject("created_at", Instant.class)
                ),
                safeSize,
                Pagination.offset(safePage, safeSize)
        );
        Long total = jdbcTemplate.queryForObject("select count(*) from content_moderation_log", Long.class);
        return ApiResponse.success(new PageResponse<>(records, safePage, safeSize, total == null ? 0 : total));
    }

    public record SensitiveWordRequest(
            @NotBlank @Size(max = 120) String word,
            @NotBlank @Size(max = 60) String category,
            @NotBlank String riskLevel,
            @NotNull Boolean enabled
    ) {
    }

    public record ModerationCheckRequest(
            @NotBlank String scene,
            @NotBlank @Size(max = 4000) String content,
            boolean blockHighRisk
    ) {
    }

    public record ModerationLogRow(
            Long id,
            String scene,
            String contentHash,
            String matchedWords,
            String riskLevel,
            String action,
            String operator,
            String traceId,
            Instant createdAt
    ) {
    }
}
