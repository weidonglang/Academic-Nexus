package weidonglang.tianshiwebside.course.mapper;

import java.time.Instant;

public record CourseOfferingDetailRow(
        Long offeringId,
        Long courseId,
        String courseCode,
        String courseName,
        Integer credit,
        String category,
        String teacherName,
        String term,
        Integer capacity,
        String scheduleText,
        String classroom,
        Instant selectionStartAt,
        Instant selectionEndAt
) {
}
