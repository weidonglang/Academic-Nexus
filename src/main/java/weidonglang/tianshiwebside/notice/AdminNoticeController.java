package weidonglang.tianshiwebside.notice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.governance.ContentModerationService;
import weidonglang.tianshiwebside.notice.mapper.NoticeMapper;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/notices")
@PreAuthorize("hasAuthority('NOTICE_WRITE')")
public class AdminNoticeController {
    private final NoticeMapper noticeMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final QueryCacheService queryCacheService;
    private final ContentModerationService moderationService;
    private final JdbcTemplate jdbcTemplate;

    public AdminNoticeController(
            NoticeMapper noticeMapper,
            NotificationService notificationService,
            AuditLogService auditLogService,
            QueryCacheService queryCacheService,
            ContentModerationService moderationService,
            JdbcTemplate jdbcTemplate
    ) {
        this.noticeMapper = noticeMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.queryCacheService = queryCacheService;
        this.moderationService = moderationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    /**
     * 功能：发布通知公告。
     * 说明：管理员填写公告标题、内容、类型和接收角色后，后端保存公告记录，
     * 并为目标用户生成通知，用于首页公告、选课通知、考试通知和审核通知展示。
     */
    public ApiResponse<NoticeMapper.NoticeRow> publish(Principal principal, @Valid @RequestBody PublishNoticeRequest request) {
        moderationService.checkConfigured("NOTICE", request.title() + "\n" + request.content(), principal.getName());
        List<Long> userIds = resolveTargetUserIds(request);
        if (userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接收人数为 0，不能发布通知");
        }
        NoticeMapper.NoticeCommand command = new NoticeMapper.NoticeCommand(
                request.title().trim(), request.content().trim(), request.category().trim(), request.pinned(),
                Instant.now(), principal.getName());
        noticeMapper.insertNotice(command);
        notificationService.notifyUsers(userIds, request.title(), request.content(), request.category(), "NOTICE", command.getId());
        auditLogService.record(principal.getName(), request.hasTargetScope() ? "PUBLISH_TARGETED_NOTICE" : "PUBLISH_NOTICE",
                "NOTICE", command.getId(), request.title() + ", target=" + request.targetSummary() + ", receivers=" + userIds.size(), null);
        queryCacheService.evictByPrefix("query:notices:");
        queryCacheService.evictByPrefix("query:notifications:");
        queryCacheService.evictByPrefix("query:dashboard:");
        return ApiResponse.success(noticeMapper.findNotices(null, 1, 0).get(0));
    }

    @GetMapping("/stats")
    /**
     * 功能：查询公告统计数据。
     * 说明：管理端公告页面用于展示不同类别公告数量和发布情况，辅助答辩说明公告管理功能完整。
     */
    public ApiResponse<List<NoticeMapper.NoticeStatRow>> stats() {
        return ApiResponse.success(noticeMapper.findNoticeStats());
    }

    private List<Long> resolveTargetUserIds(PublishNoticeRequest request) {
        String type = request.targetType() == null || request.targetType().isBlank()
                ? (request.roleCode() == null || request.roleCode().isBlank() ? "ALL" : "ROLE")
                : request.targetType().trim().toUpperCase(Locale.ROOT);
        String value = request.targetValue() == null ? "" : request.targetValue().trim();
        return switch (type) {
            case "ALL" -> noticeMapper.findAllUserIds();
            case "ROLE", "STUDENT", "TEACHER", "ADMIN" -> noticeMapper.findUserIdsByRoleCode(
                    type.equals("ROLE") ? requireValue(request.roleCode(), "角色目标不能为空") : type);
            case "GRADE" -> queryIds("""
                    select u.id
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.grade = ?
                    """, value);
            case "MAJOR" -> queryIds("""
                    select u.id
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.major = ?
                    """, value);
            case "CLASS" -> queryIds("""
                    select u.id
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.class_name = ?
                    """, value);
            case "OFFERING" -> queryIds("""
                    select distinct u.id
                    from course_selection cs
                    join student s on s.id = cs.student_id
                    join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and cs.offering_id = ?
                    """, parseLongTarget(value, "教学班 ID 必须是数字"));
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的通知目标范围: " + type);
        };
    }

    private String requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private Long parseLongTarget(String value, String message) {
        try {
            return Long.parseLong(requireValue(value, message));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private List<Long> queryIds(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
    }

    public record PublishNoticeRequest(@NotBlank String title, @NotBlank String content, @NotBlank String category,
                                       @NotNull Boolean pinned, String roleCode, String targetType, String targetValue) {
        public PublishNoticeRequest(String title, String content, String category, Boolean pinned, String roleCode) {
            this(title, content, category, pinned, roleCode, null, null);
        }

        boolean hasTargetScope() {
            return targetType != null && !targetType.isBlank();
        }

        String targetSummary() {
            if (targetType != null && !targetType.isBlank()) {
                return targetType + (targetValue == null || targetValue.isBlank() ? "" : "=" + targetValue);
            }
            return roleCode == null || roleCode.isBlank() ? "ALL" : "ROLE=" + roleCode;
        }
    }
}
