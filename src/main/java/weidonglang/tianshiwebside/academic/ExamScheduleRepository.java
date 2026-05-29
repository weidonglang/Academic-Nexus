package weidonglang.tianshiwebside.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import weidonglang.tianshiwebside.course.CourseOffering;

import java.time.LocalDateTime;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    boolean existsByCourseOfferingAndExamTime(CourseOffering courseOffering, LocalDateTime examTime);
}
