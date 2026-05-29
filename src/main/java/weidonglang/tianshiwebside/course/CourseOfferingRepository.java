package weidonglang.tianshiwebside.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    List<CourseOffering> findByTermOrderByCourseCodeAsc(String term);
}
