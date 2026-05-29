package weidonglang.tianshiwebside.information;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

import java.time.Instant;
import java.util.List;

/**
 * 教学信息查询中心。
 *
 * 这个 Controller 汇总了学业预警、毕业审核、班级课表、选课名单、教学执行计划、
 * 论文成绩和教学反馈等信息查询功能。它适合在答辩中说明：系统不是只实现登录和选课，
 * 还覆盖了教务平台常见的信息服务入口。
 */
@RestController
@RequestMapping("/api/information")
public class InformationCenterController {
    private final InformationCenterMapper informationMapper;
    private final StudentMapper studentMapper;

    public InformationCenterController(InformationCenterMapper informationMapper, StudentMapper studentMapper) {
        this.informationMapper = informationMapper;
        this.studentMapper = studentMapper;
    }

    @GetMapping("/academic-warnings/me")
    public ApiResponse<List<InformationCenterMapper.AcademicWarningRow>> academicWarnings(Authentication authentication) {
        return ApiResponse.success(informationMapper.findWarningsByUsername(authenticatedUsername(authentication)));
    }

    @GetMapping("/graduation-audit/me")
    public ApiResponse<List<InformationCenterMapper.GraduationAuditRow>> graduationAudit(Authentication authentication) {
        return ApiResponse.success(informationMapper.findGraduationAuditByUsername(authenticatedUsername(authentication)));
    }

    @GetMapping("/class-schedules")
    public ApiResponse<List<InformationCenterMapper.ClassScheduleRow>> classSchedules(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String term
    ) {
        return ApiResponse.success(informationMapper.findClassSchedule(className, term));
    }

    @GetMapping("/course-rosters")
    public ApiResponse<List<InformationCenterMapper.CourseRosterRow>> courseRosters(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) String term
    ) {
        return ApiResponse.success(informationMapper.findCourseRoster(offeringId, term));
    }

    @GetMapping("/offering-options")
    public ApiResponse<List<InformationCenterMapper.OfferingOptionRow>> offeringOptions(@RequestParam(required = false) String term) {
        return ApiResponse.success(informationMapper.findOfferingOptions(term));
    }

    @GetMapping("/academic-progress/me")
    public ApiResponse<List<InformationCenterMapper.AcademicProgressRow>> academicProgress(Authentication authentication) {
        return ApiResponse.success(informationMapper.findAcademicProgressByUsername(authenticatedUsername(authentication)));
    }

    @GetMapping("/teaching-plan")
    public ApiResponse<List<InformationCenterMapper.TeachingPlanRow>> teachingPlan(
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String grade
    ) {
        return ApiResponse.success(informationMapper.findTeachingPlan(major, grade));
    }

    @GetMapping("/weekly-schedule/me")
    public ApiResponse<List<InformationCenterMapper.WeeklyScheduleRow>> weeklySchedule(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer week
    ) {
        return ApiResponse.success(informationMapper.findWeeklySchedule(authenticatedUsername(authentication), week));
    }

    @GetMapping("/thesis-grade/me")
    public ApiResponse<List<InformationCenterMapper.ThesisGradeRow>> thesisGrade(Authentication authentication) {
        return ApiResponse.success(informationMapper.findThesisGradesByUsername(authenticatedUsername(authentication)));
    }

    @GetMapping("/feedback/me")
    public ApiResponse<List<InformationCenterMapper.TeachingFeedbackRow>> feedback(Authentication authentication) {
        return ApiResponse.success(informationMapper.findFeedbackByUsername(authenticatedUsername(authentication)));
    }

    @PostMapping("/feedback/me")
    public ApiResponse<InformationCenterMapper.TeachingFeedbackRow> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody SubmitFeedbackRequest request
    ) {
        Long studentId = studentMapper.findStudentIdByUsername(authenticatedUsername(authentication));
        if (studentId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }
        InformationCenterMapper.InsertFeedbackCommand command = new InformationCenterMapper.InsertFeedbackCommand(
                studentId,
                request.category().trim(),
                request.title().trim(),
                request.content().trim(),
                "已提交",
                Instant.now()
        );
        informationMapper.insertFeedback(command);
        return ApiResponse.success(new InformationCenterMapper.TeachingFeedbackRow(
                command.getId(),
                command.getCategory(),
                command.getTitle(),
                command.getContent(),
                command.getStatus(),
                null,
                command.getSubmittedAt(),
                null
        ));
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    public record SubmitFeedbackRequest(
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 1000) String content
    ) {
    }
}
