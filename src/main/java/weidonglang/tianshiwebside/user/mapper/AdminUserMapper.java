package weidonglang.tianshiwebside.user.mapper;

import org.apache.ibatis.annotations.*;
import weidonglang.tianshiwebside.user.UserStatus;

import java.util.List;

@Mapper
public interface AdminUserMapper {
    @Select("""
            select
              id as user_id,
              username as username,
              display_name as display_name,
              status as status,
              last_login_at as last_login_at
            from sys_user
            where (
              #{keyword} is null
              or username like #{keyword}
              or display_name like #{keyword}
            )
            order by id desc
            limit #{size} offset #{offset}
            """)
    List<AdminUserRow> findUsers(@Param("keyword") String keyword, @Param("size") int size, @Param("offset") int offset);

    @Select("""
            select count(*)
            from sys_user
            where (
              #{keyword} is null
              or username like #{keyword}
              or display_name like #{keyword}
            )
            """)
    long countUsers(@Param("keyword") String keyword);

    @Select("""
            select
              id as user_id,
              username as username,
              display_name as display_name,
              status as status,
              last_login_at as last_login_at
            from sys_user
            where id = #{userId}
            """)
    AdminUserRow findUserById(@Param("userId") Long userId);

    @Select("""
            select count(*)
            from sys_user
            where username = #{username}
            """)
    int countUserByUsername(@Param("username") String username);

    @Insert("""
            insert into sys_user (username, password_hash, display_name, status)
            values (#{username}, #{passwordHash}, #{displayName}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserInsertCommand command);

    @Update("""
            update sys_user
            set display_name = #{displayName},
                status = #{status}
            where id = #{userId}
            """)
    int updateUserProfile(
            @Param("userId") Long userId,
            @Param("displayName") String displayName,
            @Param("status") UserStatus status
    );

    @Update("""
            update sys_user
            set password_hash = #{passwordHash}
            where id = #{userId}
            """)
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Select("""
            select count(*)
            from student
            where user_id = #{userId}
            """)
    int countStudentProfiles(@Param("userId") Long userId);

    @Delete("""
            delete from user_notification
            where user_id = #{userId}
            """)
    int deleteUserNotifications(@Param("userId") Long userId);

    @Delete("""
            delete from sys_user
            where id = #{userId}
            """)
    int deleteUser(@Param("userId") Long userId);

    @Select("""
            select
              id as role_id,
              code as code,
              name as name
            from sys_role
            order by code asc
            """)
    List<AdminRoleRow> findRoles();

    @Select("""
            select count(*)
            from sys_role
            where code = #{roleCode}
            """)
    int countRoleByCode(@Param("roleCode") String roleCode);

    @Select("""
            select r.code
            from sys_role r
            join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
            order by r.code asc
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Delete("""
            delete from sys_user_role
            where user_id = #{userId}
            """)
    int deleteUserRoles(@Param("userId") Long userId);

    @Insert("""
            insert into sys_user_role (user_id, role_id)
            select #{userId}, id
            from sys_role
            where code = #{roleCode}
            """)
    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    class UserInsertCommand {
        private Long id;
        private final String username;
        private final String passwordHash;
        private final String displayName;
        private final UserStatus status;

        public UserInsertCommand(String username, String passwordHash, String displayName, UserStatus status) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.displayName = displayName;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public String getDisplayName() {
            return displayName;
        }

        public UserStatus getStatus() {
            return status;
        }
    }
}
