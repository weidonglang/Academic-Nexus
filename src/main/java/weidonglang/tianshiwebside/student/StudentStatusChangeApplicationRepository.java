package weidonglang.tianshiwebside.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentStatusChangeApplicationRepository extends JpaRepository<StudentStatusChangeApplication, Long> {
    List<StudentStatusChangeApplication> findByStudentUserUsernameOrderBySubmittedAtDesc(String username);
}
