package weidonglang.tianshiwebside.schedule;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.course.mapper.CourseSelectionReadMapper;
import weidonglang.tianshiwebside.course.mapper.CourseSelectionRow;
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class PersonalScheduleController {
    private final StudentMapper studentMapper;
    private final CourseSelectionReadMapper selectionReadMapper;
    private final QueryCacheService queryCacheService;
    private final ScheduleParser scheduleParser;

    public PersonalScheduleController(
            StudentMapper studentMapper,
            CourseSelectionReadMapper selectionReadMapper,
            QueryCacheService queryCacheService,
            ScheduleParser scheduleParser
    ) {
        this.studentMapper = studentMapper;
        this.selectionReadMapper = selectionReadMapper;
        this.queryCacheService = queryCacheService;
        this.scheduleParser = scheduleParser;
    }

    @GetMapping("/me/personal")
    public ApiResponse<List<ScheduleEntryResponse>> personalSchedule(Authentication authentication) {
        String username = authenticatedUsername(authentication);
        return ApiResponse.success(queryCacheService.get(
                "query:schedule:personal:" + username,
                Duration.ofSeconds(20),
                new TypeReference<List<ScheduleEntryResponse>>() {
                },
                () -> selectionReadMapper.findSelectedCourses(username, null, 200, 0).stream()
                        .map(this::toResponse)
                        .toList()
        ));
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (studentMapper.findStudentIdByUsername(authentication.getName()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }
        return authentication.getName();
    }

    private ScheduleEntryResponse toResponse(CourseSelectionRow row) {
        ScheduleParser.ParsedSchedule parsed = scheduleParser.parse(row.scheduleText());
        return new ScheduleEntryResponse(
                row.courseCode(),
                row.courseName(),
                row.teacherName(),
                row.classroom(),
                row.scheduleText(),
                parsed.dayOfWeek(),
                parsed.slot(),
                parsed.valid(),
                parsed.message()
        );
    }

    public record ScheduleEntryResponse(
            String courseCode,
            String courseName,
            String teacherName,
            String classroom,
            String scheduleText,
            int dayOfWeek,
            String slot,
            boolean scheduleValid,
            String scheduleMessage
    ) {
    }
}
