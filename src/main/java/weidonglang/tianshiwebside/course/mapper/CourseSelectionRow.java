package weidonglang.tianshiwebside.course.mapper;

import java.time.Instant;

public record CourseSelectionRow(
        Long selectionId,
        Long offeringId,
        String courseCode,
        String courseName,
        Integer credit,
        String teacherName,
        String scheduleText,
        String classroom,
        Instant selectedAt
) {
}
