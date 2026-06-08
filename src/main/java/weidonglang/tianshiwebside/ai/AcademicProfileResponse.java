package weidonglang.tianshiwebside.ai;

import java.util.List;

public record AcademicProfileResponse(
        String studentNo,
        String studentName,
        String college,
        String major,
        String className,
        String grade,
        String status,
        int earnedCredits,
        int plannedCredits,
        int remainingCredits,
        int failedCourseCount,
        int retakeCourseCount,
        String directionStatus,
        String graduationRiskLevel,
        String aiSuggestion,
        List<CourseRiskRow> failedCourses,
        List<ProgressRow> progress,
        List<AuditRow> graduationAudits,
        String serviceMode
) {
    public record CourseRiskRow(String courseCode, String courseName, int credit, int score, String term) {
    }

    public record ProgressRow(String courseType, int courseCount, int totalCredits, int passedCredits, double averageScore) {
    }

    public record AuditRow(String auditItem, String requiredValue, String currentValue, boolean passed, String remark) {
    }
}
