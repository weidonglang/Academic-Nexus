package weidonglang.tianshiwebside.student;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.api.PageResponse;
import weidonglang.tianshiwebside.common.api.Pagination;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.notice.NotificationService;
import weidonglang.tianshiwebside.student.mapper.AdminStatusChangeMapper;
import weidonglang.tianshiwebside.student.mapper.AdminStatusChangeRow;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@RestController
@RequestMapping("/api/admin/status-changes")
public class AdminStatusChangeController {
    private static final EnumSet<ApplicationStatus> REVIEWABLE_STATUSES = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW
    );

    private final AdminStatusChangeMapper statusChangeMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final QueryCacheService queryCacheService;

    public AdminStatusChangeController(
            AdminStatusChangeMapper statusChangeMapper,
            NotificationService notificationService,
            AuditLogService auditLogService,
            QueryCacheService queryCacheService
    ) {
        this.statusChangeMapper = statusChangeMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.queryCacheService = queryCacheService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminStatusChangeRow>> applications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        return ApiResponse.success(queryCacheService.get(
                "query:admin:status-changes:" + (status == null ? "all" : status.name()) + ":" + (normalizedKeyword == null ? "all" : normalizedKeyword)
                        + ":" + safePage + ":" + safeSize,
                Duration.ofSeconds(20),
                new TypeReference<PageResponse<AdminStatusChangeRow>>() {
                },
                () -> new PageResponse<>(
                        statusChangeMapper.findApplications(status, normalizedKeyword, safeSize, Pagination.offset(safePage, safeSize)),
                        safePage,
                        safeSize,
                        statusChangeMapper.countApplications(status, normalizedKeyword)
                )
        ));
    }

    @PostMapping("/{applicationId}/review")
    @Transactional
    @PreAuthorize("hasAuthority('STATUS_REVIEW')")
    public ApiResponse<AdminStatusChangeRow> review(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewStatusChangeRequest request
    ) {
        AdminStatusChangeRow application = statusChangeMapper.findApplicationById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学籍异动申请不存在");
        }
        if (!REVIEWABLE_STATUSES.contains(application.status())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该申请已结束，不能重复审核");
        }
        if (request.decision() == ReviewDecision.REJECT && request.comment().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回申请必须填写审核意见");
        }

        ApplicationStatus targetStatus = request.decision() == ReviewDecision.APPROVE
                ? ApplicationStatus.APPROVED
                : ApplicationStatus.REJECTED;
        statusChangeMapper.updateReviewResult(
                applicationId,
                targetStatus,
                Instant.now(),
                request.comment().trim()
        );
        if (targetStatus == ApplicationStatus.APPROVED) {
            applyStudentStatus(application);
        }
        notificationService.notifyStudent(application.studentId(), "学籍异动审核结果",
                "你的申请已" + (targetStatus == ApplicationStatus.APPROVED ? "通过" : "驳回") + "：" + request.comment(),
                "STATUS", "STATUS_CHANGE", applicationId);
        auditLogService.record(authentication.getName(), "REVIEW_STATUS_CHANGE", "STATUS_CHANGE", applicationId,
                targetStatus.name() + ": " + request.comment(), null);
        queryCacheService.evictByPrefix("query:admin:status-changes:");
        queryCacheService.evictByPrefix("query:student:status-changes:");
        queryCacheService.evictByPrefix("query:student:profile:");
        queryCacheService.evictByPrefix("query:notifications:");
        return ApiResponse.success(statusChangeMapper.findApplicationById(applicationId));
    }

    private void applyStudentStatus(AdminStatusChangeRow application) {
        String nextStatus = switch (application.type()) {
            case SUSPEND -> "休学";
            case RESUME -> "在籍";
            case TRANSFER_MAJOR, OTHER -> application.studentStatus();
        };
        if (!nextStatus.equals(application.studentStatus())) {
            statusChangeMapper.updateStudentStatus(application.studentId(), nextStatus);
        }
    }

    public enum ReviewDecision {
        APPROVE,
        REJECT
    }

    public record ReviewStatusChangeRequest(
            @NotNull ReviewDecision decision,
            @NotBlank @Size(max = 500) String comment
    ) {
    }
}
