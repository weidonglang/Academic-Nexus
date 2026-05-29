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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.student.mapper.RegistrationApplicationMapper;
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/students/me/registration-applications")
public class RegistrationApplicationController {
    private final StudentMapper studentMapper;
    private final RegistrationApplicationMapper applicationMapper;

    public RegistrationApplicationController(StudentMapper studentMapper, RegistrationApplicationMapper applicationMapper) {
        this.studentMapper = studentMapper;
        this.applicationMapper = applicationMapper;
    }

    @GetMapping
    public ApiResponse<List<RegistrationApplicationMapper.RegistrationApplicationRow>> list(
            Authentication authentication,
            @RequestParam(required = false) RegistrationApplicationType type
    ) {
        return ApiResponse.success(applicationMapper.findMine(authenticatedUsername(authentication), type));
    }

    @PostMapping
    public ApiResponse<RegistrationApplicationMapper.RegistrationApplicationRow> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitRegistrationApplicationRequest request
    ) {
        Long studentId = studentMapper.findStudentIdByUsername(authenticatedUsername(authentication));
        if (studentId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }

        RegistrationApplicationMapper.InsertRegistrationApplicationCommand command =
                new RegistrationApplicationMapper.InsertRegistrationApplicationCommand(
                        studentId,
                        request.type(),
                        request.targetName().trim(),
                        normalizeOptional(request.courseName()),
                        request.reason().trim(),
                        ApplicationStatus.SUBMITTED,
                        Instant.now()
                );
        applicationMapper.insert(command);

        return ApiResponse.success(new RegistrationApplicationMapper.RegistrationApplicationRow(
                command.getId(),
                command.getType(),
                command.getTargetName(),
                command.getCourseName(),
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

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record SubmitRegistrationApplicationRequest(
            @NotNull RegistrationApplicationType type,
            @NotBlank @Size(max = 120) String targetName,
            @Size(max = 120) String courseName,
            @NotBlank @Size(max = 500) String reason
    ) {
    }
}
