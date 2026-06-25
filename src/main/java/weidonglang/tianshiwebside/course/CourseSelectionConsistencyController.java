package weidonglang.tianshiwebside.course;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/course-selection/consistency")
@PreAuthorize("hasRole('ADMIN')")
public class CourseSelectionConsistencyController {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    public CourseSelectionConsistencyController(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            AuditLogService auditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<ConsistencyReport> report(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(buildReport(Math.min(Math.max(limit, 1), 200), false));
    }

    @PostMapping("/check")
    public ApiResponse<ConsistencyReport> check(Principal principal, @RequestParam(defaultValue = "50") int limit) {
        ConsistencyReport report = buildReport(Math.min(Math.max(limit, 1), 200), true);
        auditLogService.record(principal.getName(), "CHECK_SELECTION_CONSISTENCY", "COURSE_SELECTION",
                null, "inconsistent=" + report.inconsistentCount(), TraceIdHolder.get());
        return ApiResponse.success(report);
    }

    @PostMapping("/repair")
    public ApiResponse<ConsistencyReport> repair(Principal principal, @RequestParam(defaultValue = "50") int limit) {
        ConsistencyReport before = buildReport(Math.min(Math.max(limit, 1), 200), true);
        for (ConsistencyRow row : before.rows()) {
            redisTemplate.opsForValue().set(stockKey(row.offeringId()), String.valueOf(row.expectedStock()), Duration.ofMinutes(30));
        }
        auditLogService.record(principal.getName(), "REPAIR_SELECTION_STOCK", "COURSE_SELECTION",
                null, "count=" + before.rows().size() + ", inconsistent=" + before.inconsistentCount(), TraceIdHolder.get());
        return ApiResponse.success(buildReport(Math.min(Math.max(limit, 1), 200), true));
    }

    private ConsistencyReport buildReport(int limit, boolean checkedNow) {
        Instant checkedAt = Instant.now();
        boolean redisReachable = true;
        String redisMessage = "Redis 可用";
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
        } catch (RuntimeException ex) {
            redisReachable = false;
            redisMessage = "Redis 不可用，选课将降级到数据库容量校验";
        }
        boolean finalRedisReachable = redisReachable;
        List<ConsistencyRow> rows = jdbcTemplate.query("""
                        select
                          co.id as offering_id,
                          c.name as course_name,
                          co.capacity as capacity,
                          count(cs.id) as selected_count
                        from course_offering co
                        join course c on c.id = co.course_id
                        left join course_selection cs on cs.offering_id = co.id
                        group by co.id, c.name, co.capacity
                        order by co.id asc
                        limit ?
                        """,
                (rs, rowNum) -> {
                    long offeringId = rs.getLong("offering_id");
                    int capacity = rs.getInt("capacity");
                    int selected = rs.getInt("selected_count");
                    int expected = Math.max(0, capacity - selected);
                    Integer redisStock = finalRedisReachable ? redisStock(offeringId) : null;
                    int diff = redisStock == null ? expected : redisStock - expected;
                    return new ConsistencyRow(
                            offeringId,
                            rs.getString("course_name"),
                            capacity,
                            selected,
                            redisStock,
                            expected,
                            diff,
                            redisStock != null && diff == 0,
                            selected > capacity,
                            checkedNow ? checkedAt : null
                    );
                },
                limit
        );
        long inconsistent = rows.stream().filter(row -> !row.consistent()).count();
        boolean oversold = rows.stream().anyMatch(ConsistencyRow::oversold);
        return new ConsistencyReport(checkedAt, redisReachable, redisMessage, inconsistent, oversold, rows);
    }

    private Integer redisStock(long offeringId) {
        try {
            String value = redisTemplate.opsForValue().get(stockKey(offeringId));
            return value == null ? null : Integer.valueOf(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String stockKey(long offeringId) {
        return "selection:offering:" + offeringId + ":remaining";
    }

    public record ConsistencyReport(
            Instant checkedAt,
            boolean redisReachable,
            String redisMessage,
            long inconsistentCount,
            boolean oversoldRisk,
            List<ConsistencyRow> rows
    ) {
    }

    public record ConsistencyRow(
            long offeringId,
            String courseName,
            int capacity,
            int selectedCountInDb,
            Integer redisStock,
            int expectedStock,
            int diff,
            boolean consistent,
            boolean oversold,
            Instant lastCheckedAt
    ) {
    }
}
