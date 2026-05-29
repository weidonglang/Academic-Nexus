package weidonglang.tianshiwebside.academic;

import java.math.BigDecimal;

public record GradeRecordRow(
        String term,
        String courseCode,
        String courseName,
        Integer credit,
        String courseType,
        Integer score,
        BigDecimal gradePoint,
        String examType
) {
}
