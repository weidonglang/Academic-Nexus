package weidonglang.tianshiwebside.course;

import org.springframework.data.jpa.repository.JpaRepository;
import weidonglang.tianshiwebside.student.Student;

import java.util.List;
import java.util.Optional;

public interface CourseSelectionRepository extends JpaRepository<CourseSelection, Long> {
    boolean existsByStudentAndOffering(Student student, CourseOffering offering);

    long countByOffering(CourseOffering offering);

    List<CourseSelection> findByStudentOrderBySelectedAtDesc(Student student);

    Optional<CourseSelection> findByIdAndStudent(Long id, Student student);
}
