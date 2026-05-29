package weidonglang.tianshiwebside.evaluation.mapper;

import java.time.Instant;

public record EvaluationRecordRow(
        Long evaluationId,
        String studentNo,
        String studentName,
        String courseCode,
        String courseName,
        String teacherName,
        String term,
        Integer teachingScore,
        Integer contentScore,
        Integer interactionScore,
        Integer overallScore,
        String comment,
        Instant submittedAt
) {
}
