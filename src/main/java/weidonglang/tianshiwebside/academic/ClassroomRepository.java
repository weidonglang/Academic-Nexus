package weidonglang.tianshiwebside.academic;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    boolean existsByRoom(String room);
}
