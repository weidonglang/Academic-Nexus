package weidonglang.tianshiwebside.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    Optional<SysRole> findByCode(String code);
}
