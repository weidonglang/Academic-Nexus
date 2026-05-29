package weidonglang.tianshiwebside.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 系统用户实体，对应 sys_user 表。
 *
 * 学生、教师、管理员本质上都先是一个登录用户，再通过角色区分权限范围。
 * 用户和角色是多对多关系，登录成功后系统会根据 roles 决定菜单和接口权限。
 */
@Entity
@Table(name = "sys_user")
public class SysUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<SysRole> roles = new LinkedHashSet<>();

    protected SysUser() {
    }

    public SysUser(String username, String passwordHash, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
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

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<SysRole> getRoles() {
        return roles;
    }

    public void addRole(SysRole role) {
        roles.add(role);
    }

    public void markLoggedIn() {
        this.lastLoginAt = Instant.now();
    }
}
