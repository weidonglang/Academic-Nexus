package weidonglang.tianshiwebside.student.mapper;

import weidonglang.tianshiwebside.student.ApplicationStatus;
import weidonglang.tianshiwebside.student.StatusChangeType;

import java.time.Instant;

public record AdminStatusChangeRow(
        Long id,
        Long studentId,
        String studentNo,
        String studentName,
        String college,
        String major,
        String className,
        String studentStatus,
        StatusChangeType type,
        String reason,
        ApplicationStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewComment
) {
}
