package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SensitiveWordsAdminRegressionTests extends HttpRegressionTestSupport {
    @Test
    void adminCanLoadSensitiveWordsAndModerationLogsButStudentIsForbidden() throws Exception {
        String suffix = suffix();
        String admin = "v141_sensitive_admin_" + suffix;
        String student = "v141_sensitive_student_" + suffix;
        seedUser(admin, "敏感词管理员", List.of("ADMIN"), List.of());
        seedStudent(student, "敏感词学生");

        String adminToken = login(admin);
        JsonNode words = json(get("/api/admin/sensitive-words?page=1&size=5", adminToken), HttpStatus.OK);
        assertThat(words.at("/data/records").isArray()).isTrue();
        assertThat(words.at("/data/page").asInt()).isEqualTo(1);

        JsonNode logs = json(get("/api/admin/content-moderation/logs?page=1&size=5", adminToken), HttpStatus.OK);
        assertThat(logs.at("/data/records").isArray()).isTrue();
        assertThat(logs.at("/data/page").asInt()).isEqualTo(1);

        String studentToken = login(student);
        assertThat(get("/api/admin/sensitive-words", studentToken).statusCode())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(get("/api/admin/content-moderation/logs", studentToken).statusCode())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
