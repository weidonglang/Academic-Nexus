package weidonglang.tianshiwebside.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.notice.NotificationService;
import weidonglang.tianshiwebside.student.mapper.RegistrationApplicationMapper;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@RestController
@RequestMapping("/api/admin/registration-applications")
public class AdminRegistrationApplicationController {
    private static final EnumSet<ApplicationStatus> REVIEWABLE_STATUSES = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW
    );

    private final RegistrationApplicationMapper applicationMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AdminRegistrationApplicationController(
            RegistrationApplicationMapper applicationMapper,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.applicationMapper = applicationMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<RegistrationApplicationMapper.AdminRegistrationApplicationRow>> applications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) RegistrationApplicationType type,
            @RequestParam(required = false) String keyword
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        return ApiResponse.success(applicationMapper.findAdminApplications(status, type, normalizedKeyword));
    }

    @PostMapping("/{applicationId}/review")
    @Transactional
    @PreAuthorize("hasAuthority('STATUS_REVIEW')")
    public ApiResponse<RegistrationApplicationMapper.AdminRegistrationApplicationRow> review(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewRegistrationApplicationRequest request
    ) {
        RegistrationApplicationMapper.AdminRegistrationApplicationRow application =
                applicationMapper.findAdminApplicationById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名申请不存在");
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
        applicationMapper.updateReviewResult(applicationId, targetStatus, Instant.now(), request.comment().trim());
        notificationService.notifyStudent(application.studentId(), "报名申请审核结果",
                "你的" + application.type().name() + "申请已" + (targetStatus == ApplicationStatus.APPROVED ? "通过" : "驳回") + "：" + request.comment(),
                "APPLICATION", "REGISTRATION_APPLICATION", applicationId);
        auditLogService.record(authentication.getName(), "REVIEW_REGISTRATION_APPLICATION", "REGISTRATION_APPLICATION",
                applicationId, targetStatus.name() + ": " + request.comment(), null);
        return ApiResponse.success(applicationMapper.findAdminApplicationById(applicationId));
    }

    public enum ReviewDecision {
        APPROVE,
        REJECT
    }

    public record ReviewRegistrationApplicationRequest(
            @NotNull ReviewDecision decision,
            @NotBlank @Size(max = 500) String comment
    ) {
    }
}
