package weidonglang.tianshiwebside.schedule;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.course.mapper.CourseSelectionReadMapper;
import weidonglang.tianshiwebside.course.mapper.CourseSelectionRow;
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class PersonalScheduleController {
    private final StudentMapper studentMapper;
    private final CourseSelectionReadMapper selectionReadMapper;

    public PersonalScheduleController(
            StudentMapper studentMapper,
            CourseSelectionReadMapper selectionReadMapper
    ) {
        this.studentMapper = studentMapper;
        this.selectionReadMapper = selectionReadMapper;
    }

    @GetMapping("/me/personal")
    public ApiResponse<List<ScheduleEntryResponse>> personalSchedule(Authentication authentication) {
        String username = authenticatedUsername(authentication);
        return ApiResponse.success(selectionReadMapper.findSelectedCourses(username, 200, 0).stream()
                .map(this::toResponse)
                .toList());
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
        ParsedSchedule parsed = parseSchedule(row.scheduleText());
        return new ScheduleEntryResponse(
                row.courseCode(),
                row.courseName(),
                row.teacherName(),
                row.classroom(),
                row.scheduleText(),
                parsed.dayOfWeek(),
                parsed.slot()
        );
    }

    private ParsedSchedule parseSchedule(String scheduleText) {
        int dayOfWeek = 1;
        if (scheduleText.contains("\u5468\u4e8c")) {
            dayOfWeek = 2;
        } else if (scheduleText.contains("\u5468\u4e09")) {
            dayOfWeek = 3;
        } else if (scheduleText.contains("\u5468\u56db")) {
            dayOfWeek = 4;
        } else if (scheduleText.contains("\u5468\u4e94")) {
            dayOfWeek = 5;
        } else if (scheduleText.contains("\u5468\u516d")) {
            dayOfWeek = 6;
        } else if (scheduleText.contains("\u5468\u65e5")) {
            dayOfWeek = 7;
        }

        String slot = "1-2";
        if (scheduleText.contains("3-4")) {
            slot = "3-4";
        } else if (scheduleText.contains("5-6")) {
            slot = "5-6";
        } else if (scheduleText.contains("7-8")) {
            slot = "7-8";
        } else if (scheduleText.contains("9-10")) {
            slot = "9-10";
        }

        return new ParsedSchedule(dayOfWeek, slot);
    }

    public record ScheduleEntryResponse(
            String courseCode,
            String courseName,
            String teacherName,
            String classroom,
            String scheduleText,
            int dayOfWeek,
            String slot
    ) {
    }

    private record ParsedSchedule(int dayOfWeek, String slot) {
    }
}
