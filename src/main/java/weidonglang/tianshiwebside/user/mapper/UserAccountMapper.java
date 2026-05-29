package weidonglang.tianshiwebside.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import weidonglang.tianshiwebside.user.UserStatus;

import java.time.Instant;
import java.util.List;

@Mapper
public interface UserAccountMapper {
    @Select("""
            select
              id as id,
              username as username,
              password_hash as password_hash,
              display_name as display_name,
              status as status
            from sys_user
            where username = #{username}
            """)
    UserAccountRow findByUsername(@Param("username") String username);

    @Select("""
            select r.code
            from sys_role r
            join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
            order by r.code asc
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            select p.code
            from sys_permission p
            join sys_role_permission rp on rp.permission_id = p.id
            join sys_user_role ur on ur.role_id = rp.role_id
            where ur.user_id = #{userId}
            order by p.code asc
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            select count(*)
            from sys_user
            where username = #{username}
            """)
    int countByUsername(@Param("username") String username);

    @Update("""
            update sys_user
            set last_login_at = #{lastLoginAt}
            where id = #{userId}
            """)
    int updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") Instant lastLoginAt);

    record UserAccountRow(
            Long id,
            String username,
            String passwordHash,
            String displayName,
            UserStatus status
    ) {
    }
}
