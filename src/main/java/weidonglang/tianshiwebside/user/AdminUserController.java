package weidonglang.tianshiwebside.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.user.mapper.AdminRoleRow;
import weidonglang.tianshiwebside.user.mapper.AdminUserMapper;
import weidonglang.tianshiwebside.user.mapper.AdminUserRow;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminUserController(AdminUserMapper userMapper, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    /**
     * 功能：分页查询系统用户。
     * 说明：管理端“用户与角色”页面通过页码、每页条数和关键字查询账号，
     * 避免一次性加载大量学生、教师和管理员账号导致页面卡顿。
     */
    public ApiResponse<UserPageResponse<AdminUserResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(10, Math.min(size, 200));
        int offset = (safePage - 1) * safeSize;
        List<AdminUserResponse> records = userMapper.findUsers(normalizedKeyword, safeSize, offset).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(new UserPageResponse<>(records, safePage, safeSize, userMapper.countUsers(normalizedKeyword)));
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleRow>> roles() {
        return ApiResponse.success(userMapper.findRoles());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    /**
     * 功能：新增系统用户。
     * 说明：管理员提交账号、姓名、初始密码和角色，后端校验账号是否重复，
     * 使用 BCrypt 保存密码，并写入用户角色关系。
     */
    public ApiResponse<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        String username = request.username().trim();
        if (userMapper.countUserByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        validateRoleCodes(request.roleCodes());

        AdminUserMapper.UserInsertCommand command = new AdminUserMapper.UserInsertCommand(
                username,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                UserStatus.ACTIVE
        );
        userMapper.insertUser(command);
        updateUserRoles(command.getId(), request.roleCodes());
        return ApiResponse.success(toResponse(userMapper.findUserById(command.getId())));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    /**
     * 功能：修改用户基础信息。
     * 说明：管理员可更新显示姓名和账号状态；为了防止误操作，当前登录账号不能把自己禁用或锁定。
     */
    public ApiResponse<AdminUserResponse> updateUser(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        AdminUserRow user = requireUser(userId);
        if (isSelf(authentication, user.username()) && request.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能禁用或锁定当前登录账号");
        }
        userMapper.updateUserProfile(userId, request.displayName().trim(), request.status());
        return ApiResponse.success(toResponse(userMapper.findUserById(userId)));
    }

    @PutMapping("/{userId}/roles")
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    /**
     * 功能：更新用户角色。
     * 说明：管理员为用户重新分配学生、教师、管理员等角色，
     * 角色变化会影响用户登录后的菜单范围和接口访问权限。
     */
    public ApiResponse<AdminUserResponse> updateRoles(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        AdminUserRow user = requireUser(userId);
        validateRoleCodes(request.roleCodes());
        if (isSelf(authentication, user.username()) && !request.roleCodes().contains("ADMIN")) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能移除当前登录账号的管理员角色");
        }
        updateUserRoles(userId, request.roleCodes());
        auditLogService.record(authentication.getName(), "UPDATE_USER_ROLES", "USER", userId, String.join(",", request.roleCodes()), null);
        return ApiResponse.success(toResponse(userMapper.findUserById(userId)));
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    /**
     * 功能：重置用户密码。
     * 说明：管理员为指定用户设置新密码，后端加密后更新密码字段，
     * 用户下次登录时必须使用新密码。
     */
    public ApiResponse<Void> resetPassword(Authentication authentication, @PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        requireUser(userId);
        userMapper.updatePassword(userId, passwordEncoder.encode(request.password()));
        auditLogService.record(authentication.getName(), "RESET_PASSWORD", "USER", userId, null, null);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}")
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    /**
     * 功能：删除系统用户。
     * 说明：删除前会校验不能删除当前登录账号，并阻止删除已有学生档案和业务数据的账号，
     * 避免破坏选课、成绩、学籍等历史数据关联。
     */
    public ApiResponse<Void> deleteUser(Authentication authentication, @PathVariable Long userId) {
        AdminUserRow user = requireUser(userId);
        if (isSelf(authentication, user.username())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能删除当前登录账号");
        }
        if (userMapper.countStudentProfiles(userId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该账号已有学生档案和业务数据，请先禁用账号，不建议物理删除");
        }
        userMapper.deleteUserNotifications(userId);
        userMapper.deleteUserRoles(userId);
        userMapper.deleteUser(userId);
        auditLogService.record(authentication.getName(), "DELETE_USER", "USER", userId, user.username(), null);
        return ApiResponse.success();
    }

    private AdminUserRow requireUser(Long userId) {
        AdminUserRow user = userMapper.findUserById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return user;
    }

    private void validateRoleCodes(List<String> roleCodes) {
        for (String roleCode : roleCodes) {
            if (userMapper.countRoleByCode(roleCode) == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在: " + roleCode);
            }
        }
    }

    private void updateUserRoles(Long userId, List<String> roleCodes) {
        userMapper.deleteUserRoles(userId);
        roleCodes.stream()
                .distinct()
                .forEach(roleCode -> userMapper.insertUserRole(userId, roleCode));
    }

    private AdminUserResponse toResponse(AdminUserRow row) {
        return new AdminUserResponse(
                row.userId(),
                row.username(),
                row.displayName(),
                row.status(),
                row.lastLoginAt(),
                userMapper.findRoleCodesByUserId(row.userId())
        );
    }

    private boolean isSelf(Authentication authentication, String username) {
        return authentication != null && username.equals(authentication.getName());
    }

    public record AdminUserResponse(
            Long userId,
            String username,
            String displayName,
            UserStatus status,
            Instant lastLoginAt,
            List<String> roleCodes
    ) {
    }

    public record UserPageResponse<T>(
            List<T> records,
            int page,
            int size,
            long total
    ) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 64) String displayName,
            @NotBlank @Size(min = 6, max = 64) String password,
            @NotNull List<String> roleCodes
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 64) String displayName,
            @NotNull UserStatus status
    ) {
    }

    public record UpdateUserRolesRequest(
            @NotNull List<String> roleCodes
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 6, max = 64) String password
    ) {
    }
}
