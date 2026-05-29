package weidonglang.tianshiwebside.evaluation.mapper;

import java.time.Instant;

public record EvaluationTaskRow(
        Long offeringId,
        Long selectionId,
        String courseCode,
        String courseName,
        String teacherName,
        String term,
        String scheduleText,
        String classroom,
        Boolean evaluated,
        Integer teachingScore,
        Integer contentScore,
        Integer interactionScore,
        Integer overallScore,
        String comment,
        Instant submittedAt
) {
}
