package weidonglang.tianshiwebside.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByStudentNo(String studentNo);

    Optional<Student> findByStudentNo(String studentNo);

    Optional<Student> findByUserUsername(String username);
}
