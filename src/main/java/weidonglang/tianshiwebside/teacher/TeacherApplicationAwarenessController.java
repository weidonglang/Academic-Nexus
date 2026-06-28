package weidonglang.tianshiwebside.teacher;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/application-awareness")
public class TeacherApplicationAwarenessController {
    private final JdbcTemplate jdbcTemplate;
    private final QueryCacheService queryCacheService;

    public TeacherApplicationAwarenessController(JdbcTemplate jdbcTemplate, QueryCacheService queryCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryCacheService = queryCacheService;
    }

    @GetMapping("/classes/{classId}")
    public ApiResponse<ClassApplicationAwareness> classApplications(Authentication authentication, @PathVariable Long classId) {
        String username = username(authentication);
        if (count("""
                select count(*)
                from academic_class
                where id = ? and homeroom_teacher_user_id = (select id from sys_user where username = ?)
                """, classId, username) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看本人负责班级的申请概要");
        }
        return ApiResponse.success(queryCacheService.get(
                "query:teacher:application-awareness:class:" + username + ":" + classId,
                Duration.ofSeconds(20),
                new TypeReference<ClassApplicationAwareness>() {
                },
                () -> new ClassApplicationAwareness(
                        statusChanges(classId),
                        registrationsByClass(classId)
                )
        ));
    }

    @GetMapping("/registrations")
    public ApiResponse<List<ApplicationAwarenessRow>> courseRegistrations(Authentication authentication) {
        String username = username(authentication);
        String displayName = displayName(username);
        return ApiResponse.success(queryCacheService.get(
                "query:teacher:application-awareness:registrations:" + username,
                Duration.ofSeconds(20),
                new TypeReference<List<ApplicationAwarenessRow>>() {
                },
                () -> jdbcTemplate.query("""
                                select a.id, s.student_no, u.display_name as student_name, s.class_name,
                                       a.type, a.target_name, a.course_name, a.status, a.submitted_at, a.reviewed_at, a.review_comment
                                from student_registration_application a
                                join student s on s.id = a.student_id
                                join sys_user u on u.id = s.user_id
                                where exists (
                                  select 1
                                  from course_offering co
                                  join course c on c.id = co.course_id
                                  where co.teacher_name = ?
                                    and (a.course_name = c.name or a.target_name = c.name or a.target_name = co.term)
                                )
                                order by a.submitted_at desc
                                limit 100
                                """,
                        (rs, rowNum) -> new ApplicationAwarenessRow(
                                rs.getLong("id"),
                                rs.getString("student_no"),
                                rs.getString("student_name"),
                                rs.getString("class_name"),
                                "REGISTRATION_APPLICATION",
                                rs.getString("type"),
                                rs.getString("target_name"),
                                rs.getString("course_name"),
                                rs.getString("status"),
                                rs.getTimestamp("submitted_at").toInstant(),
                                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant(),
                                rs.getString("review_comment")
                        ),
                        displayName)
        ));
    }

    private List<ApplicationAwarenessRow> statusChanges(Long classId) {
        return jdbcTemplate.query("""
                        select a.id, s.student_no, u.display_name as student_name, s.class_name,
                               a.type, a.reason as target_name, null as course_name, a.status, a.submitted_at, a.reviewed_at, a.review_comment
                        from student_status_change_application a
                        join student s on s.id = a.student_id
                        join sys_user u on u.id = s.user_id
                        join academic_class ac on ac.class_name = s.class_name
                        where ac.id = ?
                        order by a.submitted_at desc
                        limit 100
                        """,
                (rs, rowNum) -> new ApplicationAwarenessRow(
                        rs.getLong("id"),
                        rs.getString("student_no"),
                        rs.getString("student_name"),
                        rs.getString("class_name"),
                        "STATUS_CHANGE",
                        rs.getString("type"),
                        rs.getString("target_name"),
                        rs.getString("course_name"),
                        rs.getString("status"),
                        rs.getTimestamp("submitted_at").toInstant(),
                        rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant(),
                        rs.getString("review_comment")
                ),
                classId);
    }

    private List<ApplicationAwarenessRow> registrationsByClass(Long classId) {
        return jdbcTemplate.query("""
                        select a.id, s.student_no, u.display_name as student_name, s.class_name,
                               a.type, a.target_name, a.course_name, a.status, a.submitted_at, a.reviewed_at, a.review_comment
                        from student_registration_application a
                        join student s on s.id = a.student_id
                        join sys_user u on u.id = s.user_id
                        join academic_class ac on ac.class_name = s.class_name
                        where ac.id = ?
                        order by a.submitted_at desc
                        limit 100
                        """,
                (rs, rowNum) -> new ApplicationAwarenessRow(
                        rs.getLong("id"),
                        rs.getString("student_no"),
                        rs.getString("student_name"),
                        rs.getString("class_name"),
                        "REGISTRATION_APPLICATION",
                        rs.getString("type"),
                        rs.getString("target_name"),
                        rs.getString("course_name"),
                        rs.getString("status"),
                        rs.getTimestamp("submitted_at").toInstant(),
                        rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant(),
                        rs.getString("review_comment")
                ),
                classId);
    }

    private String displayName(String username) {
        List<String> rows = jdbcTemplate.query("select display_name from sys_user where username = ?",
                (rs, rowNum) -> rs.getString(1), username);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "教师账号不存在");
        }
        return rows.get(0);
    }

    private int count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count.intValue();
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    public record ClassApplicationAwareness(List<ApplicationAwarenessRow> statusChanges,
                                            List<ApplicationAwarenessRow> registrations) {
    }

    public record ApplicationAwarenessRow(Long id, String studentNo, String studentName, String className,
                                          String businessType, String applicationType, String targetName,
                                          String courseName, String status, Instant submittedAt,
                                          Instant reviewedAt, String reviewComment) {
    }
}
