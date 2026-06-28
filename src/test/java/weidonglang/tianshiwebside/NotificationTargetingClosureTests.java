package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationTargetingClosureTests extends HttpRegressionTestSupport {
    @Test
    void classTargetedNoticeOnlyReachesTargetStudents() throws Exception {
        String suffix = suffix();
        String admin = "notice_target_admin_" + suffix;
        String targetStudent = "notice_target_student_" + suffix;
        String otherStudent = "notice_other_student_" + suffix;
        String className = "通知目标班级-" + suffix;
        seedUser(admin, "通知管理员", List.of("ADMIN"), List.of("NOTICE_WRITE"));
        seedStudent(targetStudent, "目标学生");
        seedStudent(otherStudent, "非目标学生");
        jdbcTemplate.update("update student set class_name = ? where student_no = ?", className, targetStudent);
        jdbcTemplate.update("update student set class_name = ? where student_no = ?", className + "-其他", otherStudent);

        String token = login(admin);
        JsonNode preview = json(post("/api/admin/notices/target-preview", token, """
                {"targetType":"CLASS","targetValue":"%s"}
                """.formatted(className)), HttpStatus.OK);
        assertThat(preview.at("/data/receiverCount").asInt()).isEqualTo(1);

        json(post("/api/admin/notices", token, """
                {"title":"班级目标通知","content":"只发给目标班","category":"GENERAL","pinned":false,"targetType":"CLASS","targetValue":"%s"}
                """.formatted(className)), HttpStatus.OK);

        assertThat(count("""
                select count(*)
                from user_notification
                where user_id = (select user_id from student where student_no = ?) and title = '班级目标通知'
                """, targetStudent)).isEqualTo(1);
        assertThat(count("""
                select count(*)
                from user_notification
                where user_id = (select user_id from student where student_no = ?) and title = '班级目标通知'
                """, otherStudent)).isZero();
        assertThat(count("select count(*) from operation_audit_log where action = 'PUBLISH_TARGETED_NOTICE' and operator = ?", admin))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void targetPreviewRejectsInvalidOfferingId() throws Exception {
        String admin = "notice_invalid_admin_" + suffix();
        seedUser(admin, "通知管理员", List.of("ADMIN"), List.of("NOTICE_WRITE"));

        assertThat(post("/api/admin/notices/target-preview", login(admin), """
                {"targetType":"OFFERING","targetValue":"not-number"}
                """).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private int count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
