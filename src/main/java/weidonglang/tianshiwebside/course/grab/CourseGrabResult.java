package weidonglang.tianshiwebside.course.grab;

import java.time.Instant;

public record CourseGrabResult(
        Long selectionId,
        Long offeringId,
        String courseCode,
        String courseName,
        Integer credit,
        String teacherName,
        String scheduleText,
        String classroom,
        Instant selectedAt,
        String status,
        CourseGrabFailureReason failureReason,
        String message
) {
}
