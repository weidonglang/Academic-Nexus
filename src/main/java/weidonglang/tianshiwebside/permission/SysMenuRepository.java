package weidonglang.tianshiwebside.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    boolean existsByCode(String code);

    List<SysMenu> findAllByOrderBySortOrderAsc();
}
