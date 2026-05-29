package weidonglang.tianshiwebside.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.course.grab.LocalCourseGrabService;
import weidonglang.tianshiwebside.course.mapper.CourseSelectionWriteMapper;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@Profile({"dev", "demo"})
@RequestMapping("/api/test/course-selection")
public class CourseSelectionLoadTestController {
    private final CourseSelectionWriteMapper selectionWriteMapper;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final LocalCourseGrabService localCourseGrabService;

    public CourseSelectionLoadTestController(
            CourseSelectionWriteMapper selectionWriteMapper,
            StringRedisTemplate redisTemplate,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            LocalCourseGrabService localCourseGrabService
    ) {
        this.selectionWriteMapper = selectionWriteMapper;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.localCourseGrabService = localCourseGrabService;
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> ensureAccounts(@Valid @RequestBody AccountBatchRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(doEnsureAccounts(request)));
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(
                            "500",
                            "Load-test account preparation failed: " + rootMessage(ex),
                            null
                    ));
        }
    }

    private AccountBatchResponse doEnsureAccounts(AccountBatchRequest request) {
        String prefix = normalizePrefix(request.prefix());
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Load-test account password is required.");
        }
        String password = request.password();
        int startIndex = request.startIndex() == null ? 1 : request.startIndex();
        Long studentRoleId = ensureStudentRole();
        String passwordHash = passwordEncoder.encode(password);

        int createdUsers = 0;
        int existingUsers = 0;
        int createdStudents = 0;
        for (int offset = 0; offset < request.count(); offset++) {
            int serial = startIndex + offset;
            String username = loadUsername(prefix, serial);
            Long userId = findUserId(username);
            if (userId == null) {
                jdbcTemplate.update("""
                        insert into sys_user (username, password_hash, display_name, status)
                        values (?, ?, ?, ?)
                        """, username, passwordHash, "Load Test Student " + serial, "ACTIVE");
                userId = findUserId(username);
                createdUsers++;
            } else {
                existingUsers++;
            }
            ensureUserRole(userId, studentRoleId);

            if (!studentExists(username)) {
                jdbcTemplate.update("""
                        insert into student
                          (user_id, student_no, college, major, class_name, grade, status, phone, email, address)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        userId,
                        username,
                        "Load Test College",
                        "Course Grab Simulation",
                        "Load Test Class",
                        "2026",
                        "Active",
                        "139" + String.format("%08d", serial % 100000000),
                        username + "@load.test",
                        "Local load test data"
                );
                createdStudents++;
            }
        }

        String firstUsername = loadUsername(prefix, startIndex);
        String lastUsername = loadUsername(prefix, startIndex + request.count() - 1);
        return new AccountBatchResponse(
                prefix,
                startIndex,
                request.count(),
                firstUsername,
                lastUsername,
                createdUsers,
                existingUsers,
                createdStudents
        );
    }

    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CleanupResponse> cleanup(@Valid @RequestBody CleanupRequest request) {
        int deleted = selectionWriteMapper.deleteLoadTestSelections(
                request.offeringId(),
                request.selectedAfter(),
                request.usernames().stream().distinct().toList()
        );
        localCourseGrabService.evictOfferingStock(request.offeringId());
        return ApiResponse.success(new CleanupResponse(deleted));
    }

    @PostMapping("/redis-stock/prewarm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PrewarmStockResponse> prewarmRedisStock(@Valid @RequestBody PrewarmStockRequest request) {
        List<PrewarmStockItem> items = new ArrayList<>();
        for (Long offeringId : request.offeringIds().stream().distinct().toList()) {
            long remaining = localCourseGrabService.prewarmOfferingStock(offeringId);
            // 压测开始前预热 Redis 库存，并把 key 返回给压测报告，方便确认 Redis 参与了抢课。
            items.add(new PrewarmStockItem(
                    offeringId,
                    "selection:offering:" + offeringId + ":remaining",
                    remaining
            ));
        }
        return ApiResponse.success(new PrewarmStockResponse(items.size(), items));
    }

    @PostMapping("/redis-mode")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RedisModeResponse> redisMode(@Valid @RequestBody RedisModeRequest request) {
        localCourseGrabService.setRedisEnabled(request.enabled());
        return ApiResponse.success(new RedisModeResponse(localCourseGrabService.isRedisEnabled()));
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "lt";
        }
        String normalized = prefix.trim().replaceAll("[^A-Za-z0-9_-]", "");
        return normalized.isBlank() ? "lt" : normalized;
    }

    private String loadUsername(String prefix, int serial) {
        return prefix + String.format("%05d", serial);
    }

    private Long ensureStudentRole() {
        Long roleId = findRoleId("STUDENT");
        if (roleId != null) {
            return roleId;
        }
        jdbcTemplate.update("insert into sys_role (code, name) values (?, ?)", "STUDENT", "Student");
        return findRoleId("STUDENT");
    }

    private Long findRoleId(String code) {
        try {
            return jdbcTemplate.queryForObject("select id from sys_role where code = ?", Long.class, code);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private Long findUserId(String username) {
        try {
            return jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, username);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private void ensureUserRole(Long userId, Long roleId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user_role where user_id = ? and role_id = ?",
                Integer.class,
                userId,
                roleId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
        }
    }

    private boolean studentExists(String studentNo) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from student where student_no = ?", Integer.class, studentNo);
        return count != null && count > 0;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        if (current instanceof SQLException sqlException) {
            return sqlException.getSQLState() + "/" + sqlException.getErrorCode() + " " + sqlException.getMessage();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    public record AccountBatchRequest(
            String prefix,
            @Min(1) @Max(50000) int count,
            @Min(1) Integer startIndex,
            String password
    ) {
    }

    public record AccountBatchResponse(
            String prefix,
            int startIndex,
            int count,
            String firstUsername,
            String lastUsername,
            int createdUsers,
            int existingUsers,
            int createdStudents
    ) {
    }

    public record CleanupRequest(
            @NotNull Long offeringId,
            @NotNull Instant selectedAfter,
            @NotEmpty List<String> usernames
    ) {
    }

    public record CleanupResponse(int deleted) {
    }

    public record PrewarmStockRequest(
            @NotEmpty List<Long> offeringIds
    ) {
    }

    public record PrewarmStockResponse(
            int count,
            List<PrewarmStockItem> items
    ) {
    }

    public record PrewarmStockItem(
            Long offeringId,
            String redisKey,
            long remaining
    ) {
    }

    public record RedisModeRequest(boolean enabled) {
    }

    public record RedisModeResponse(boolean enabled) {
    }
}
