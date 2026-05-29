package weidonglang.tianshiwebside.course.mapper;

import java.time.Instant;

public record AdminCourseOfferingRow(
        Long offeringId,
        Long courseId,
        String courseCode,
        String courseName,
        Integer credit,
        String category,
        String teacherName,
        String term,
        Integer capacity,
        Long selectedCount,
        String scheduleText,
        String classroom,
        Instant selectionStartAt,
        Instant selectionEndAt
) {
}
