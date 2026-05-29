package weidonglang.tianshiwebside.academic;

import java.time.LocalDateTime;

public record ExamScheduleRow(
        String term,
        String courseCode,
        String courseName,
        LocalDateTime examTime,
        String room,
        String seatNo,
        String examType,
        String status
) {
}
