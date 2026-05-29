package weidonglang.tianshiwebside.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/students/me/status-changes")
public class StudentStatusChangeController {
    private final StudentMapper studentMapper;

    public StudentStatusChangeController(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    @GetMapping
    public ApiResponse<List<StatusChangeApplicationResponse>> list(Authentication authentication) {
        String username = authenticatedUsername(authentication);
        return ApiResponse.success(studentMapper.findStatusChangesByUsername(username)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping
    public ApiResponse<StatusChangeApplicationResponse> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitStatusChangeRequest request
    ) {
        Long studentId = studentMapper.findStudentIdByUsername(authenticatedUsername(authentication));
        if (studentId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }

        StudentMapper.InsertStatusChangeCommand command = new StudentMapper.InsertStatusChangeCommand(
                studentId,
                request.type(),
                request.reason().trim(),
                ApplicationStatus.SUBMITTED,
                Instant.now()
        );
        studentMapper.insertStatusChange(command);

        return ApiResponse.success(new StatusChangeApplicationResponse(
                command.getId(),
                command.getType(),
                command.getReason(),
                command.getStatus(),
                command.getSubmittedAt(),
                null,
                null
        ));
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    private StatusChangeApplicationResponse toResponse(StudentMapper.StatusChangeApplicationRow application) {
        return new StatusChangeApplicationResponse(
                application.id(),
                application.type(),
                application.reason(),
                application.status(),
                application.submittedAt(),
                application.reviewedAt(),
                application.reviewComment()
        );
    }

    public record SubmitStatusChangeRequest(
            @NotNull StatusChangeType type,
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record StatusChangeApplicationResponse(
            Long id,
            StatusChangeType type,
            String reason,
            ApplicationStatus status,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewComment
    ) {
    }
}
