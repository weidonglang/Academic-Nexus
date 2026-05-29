package weidonglang.tianshiwebside.evaluation.mapper;

public record EvaluationSummaryRow(
        Long offeringId,
        String courseCode,
        String courseName,
        String teacherName,
        String term,
        Long selectedCount,
        Long submittedCount,
        Double averageTeachingScore,
        Double averageContentScore,
        Double averageInteractionScore,
        Double averageOverallScore
) {
}
