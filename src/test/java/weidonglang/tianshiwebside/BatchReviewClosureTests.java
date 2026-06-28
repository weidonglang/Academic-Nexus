package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BatchReviewClosureTests extends HttpRegressionTestSupport {
    @Test
    void statusChangeBatchReviewUpdatesStateTaskAuditAndNotification() throws Exception {
        String suffix = suffix();
        String admin = "batch_review_admin_" + suffix;
        String student = "batch_review_student_" + suffix;
        seedUser(admin, "批量审核管理员", List.of("ADMIN"), List.of("STATUS_REVIEW"));
        seedStudent(student, "批量审核学生");
        Long studentId = jdbcTemplate.queryForObject("select id from student where student_no = ?", Long.class, student);
        Long submittedId = insertStatusChange(studentId, "SUSPEND", "SUBMITTED");
        Long approvedId = insertStatusChange(studentId, "OTHER", "APPROVED");

        JsonNode result = json(post("/api/admin/status-changes/batch-review", login(admin), """
                {"ids":[%d,%d],"decision":"APPROVE","comment":"批量通过"}
                """.formatted(submittedId, approvedId)), HttpStatus.OK);

        assertThat(result.at("/data/successCount").asInt()).isEqualTo(1);
        assertThat(result.at("/data/failureCount").asInt()).isEqualTo(1);
        assertThat(result.at("/data/items").toString()).contains("该申请已审核");
        assertThat(value("select status from student_status_change_application where id = ?", submittedId)).isEqualTo("APPROVED");
        assertThat(value("select status from student where id = ?", studentId)).isEqualTo("休学");
        assertThat(count("select count(*) from batch_task where task_type = 'STATUS_CHANGE_BATCH_REVIEW' and operator = ?", admin))
                .isGreaterThanOrEqualTo(1);
        assertThat(count("select count(*) from operation_audit_log where action = 'BATCH_REVIEW_STATUS_CHANGE' and operator = ?", admin))
                .isGreaterThanOrEqualTo(1);
        assertThat(count("select count(*) from user_notification where user_id = (select user_id from student where id = ?)", studentId))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void batchRejectRequiresReasonAndStudentsAreForbidden() throws Exception {
        String suffix = suffix();
        String admin = "batch_reject_admin_" + suffix;
        String student = "batch_reject_student_" + suffix;
        seedUser(admin, "批量审核管理员", List.of("ADMIN"), List.of("STATUS_REVIEW"));
        seedStudent(student, "批量审核学生");
        Long studentId = jdbcTemplate.queryForObject("select id from student where student_no = ?", Long.class, student);
        Long applicationId = insertRegistration(studentId, "SUBMITTED");

        assertThat(post("/api/admin/registration-applications/batch-review", login(admin), """
                {"ids":[%d],"decision":"REJECT","comment":""}
                """.formatted(applicationId)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(post("/api/admin/registration-applications/batch-review", login(student), """
                {"ids":[%d],"decision":"APPROVE","comment":"学生越权"}
                """.formatted(applicationId)).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private Long insertStatusChange(Long studentId, String type, String status) {
        jdbcTemplate.update("""
                        insert into student_status_change_application
                          (student_id, type, reason, status, submitted_at)
                        values (?, ?, ?, ?, ?)
                        """, studentId, type, "批量审核测试", status, Instant.now());
        return jdbcTemplate.queryForObject("select max(id) from student_status_change_application where student_id = ?", Long.class, studentId);
    }

    private Long insertRegistration(Long studentId, String status) {
        jdbcTemplate.update("""
                        insert into student_registration_application
                          (student_id, type, target_name, course_name, reason, status, submitted_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """, studentId, "RETAKE_REGISTRATION", "批量报名目标", "批量课程", "批量审核测试", status, Instant.now());
        return jdbcTemplate.queryForObject("select max(id) from student_registration_application where student_id = ?", Long.class, studentId);
    }

    private String value(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    private int count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
