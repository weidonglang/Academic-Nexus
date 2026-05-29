package weidonglang.tianshiwebside.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import weidonglang.tianshiwebside.course.Course;
import weidonglang.tianshiwebside.student.Student;

public interface AcademicGradeRepository extends JpaRepository<AcademicGrade, Long> {
    boolean existsByStudentAndCourseAndTermAndExamType(Student student, Course course, String term, String examType);
}
