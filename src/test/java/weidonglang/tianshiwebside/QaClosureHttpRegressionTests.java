package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QaClosureHttpRegressionTests {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void issue39StudentApplicationNoticeAndAiConfigEndpointsDoNotReturn500() throws Exception {
        String suffix = suffix();
        String admin = "qa_admin_" + suffix;
        String student = "qa_student_" + suffix;
        seedUser(admin, "QA 管理员", List.of("ADMIN"), List.of("NOTICE_WRITE"));
        seedStudent(student, "QA 学生");

        String adminToken = login(admin);
        String studentToken = login(student);

        JsonNode statusChange = json(post("/api/students/me/status-changes", studentToken, """
                {"type":"OTHER","reason":"QA 学籍异动申请"}
                """), HttpStatus.OK);
        assertThat(statusChange.at("/data/id").isNumber()).isTrue();

        JsonNode registration = json(post("/api/students/me/registration-applications", studentToken, """
                {"type":"RETAKE_REGISTRATION","targetName":"重修目标","courseName":"高等数学","reason":"QA 报名申请"}
                """), HttpStatus.OK);
        assertThat(registration.at("/data/id").isNumber()).isTrue();

        JsonNode notice = json(post("/api/admin/notices", adminToken, """
                {"title":"QA 公告","content":"QA 公告内容","category":"GENERAL","pinned":false,"roleCode":"STUDENT"}
                """), HttpStatus.OK);
        assertThat(notice.at("/data/title").asText()).isEqualTo("QA 公告");

        JsonNode safety = json(get("/api/admin/ai/safety/config", adminToken), HttpStatus.OK);
        assertThat(safety.at("/data").isArray()).isTrue();

        JsonNode updatedSafety = json(put("/api/admin/ai/safety/config", adminToken, """
                {"configs":[{"scene":"STUDENT_CONTENT","enabled":true,"strategy":"warn","description":"QA strategy"}]}
                """), HttpStatus.OK);
        assertThat(updatedSafety.at("/data").isArray()).isTrue();
    }

    @Test
    void issue41AccessMatrixAndMultiRolePriorityAreStable() throws Exception {
        String suffix = suffix();
        String student = "qa_only_student_" + suffix;
        String multi = "qa_multi_" + suffix;
        seedStudent(student, "单角色学生");
        seedStudent(multi, "多角色用户");
        addRoles(multi, List.of("ADMIN", "TEACHER"));

        String studentToken = login(student);
        String multiToken = login(multi);

        assertThat(get("/api/admin/ai/safety/config", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/admin/ai/safety/config", studentToken).statusCode()).isEqualTo(403);
        assertThat(get("/api/teacher/classes", studentToken).statusCode()).isEqualTo(403);

        JsonNode login = json(post("/api/auth/login", null, """
                {"username":"%s","password":"123456"}
                """.formatted(multi)), HttpStatus.OK);
        JsonNode roles = login.at("/data/user/roles");
        assertThat(roles.get(0).asText()).isEqualTo("ADMIN");
        assertThat(roles.toString()).contains("TEACHER", "STUDENT");

        assertThat(get("/api/admin/ai/safety/config", multiToken).statusCode()).isEqualTo(200);
    }

    @Test
    void issue44AiStatusReturnsReadableFallbackWhenServiceDiscoveryHasNoInstance() throws Exception {
        String suffix = suffix();
        String admin = "qa_ai_admin_" + suffix;
        seedUser(admin, "QA AI 管理员", List.of("ADMIN"), List.of());

        JsonNode status = json(get("/api/ai/status", login(admin)), HttpStatus.OK);
        assertThat(status.at("/data/currentMode").asText()).isEqualTo("主系统本地兜底模式");
        assertThat(status.at("/data/aiServiceOnline").asBoolean()).isFalse();
        assertThat(status.at("/data/lastError").asText()).isNotBlank();
    }

    private String login(String username) throws Exception {
        JsonNode body = json(post("/api/auth/login", null, """
                {"username":"%s","password":"123456"}
                """.formatted(username)), HttpStatus.OK);
        return body.at("/data/accessToken").asText();
    }

    private JsonNode json(HttpResponse<String> response, HttpStatus expectedStatus) throws Exception {
        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
        authorize(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void authorize(HttpRequest.Builder builder, String token) {
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private void seedStudent(String username, String displayName) {
        seedUser(username, displayName, List.of("STUDENT"), List.of());
        Long userId = jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, username);
        jdbcTemplate.update("""
                        insert into student (user_id, student_no, college, major, class_name, grade, status, phone, email, address)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId, username, "信息工程学院", "软件工程", "软件工程 23-1", "2023", "在籍",
                "13800000000", username + "@example.com", "天津");
    }

    private void seedUser(String username, String displayName, List<String> roleCodes, List<String> permissionCodes) {
        jdbcTemplate.update("""
                        insert into sys_user (username, password_hash, display_name, status)
                        values (?, ?, ?, ?)
                        """,
                username, passwordEncoder.encode("123456"), displayName, "ACTIVE");
        addRoles(username, roleCodes);
        if (!permissionCodes.isEmpty()) {
            addPermissions(roleCodes.get(0), permissionCodes);
        }
    }

    private void addRoles(String username, List<String> roleCodes) {
        Long userId = jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, username);
        for (String roleCode : roleCodes) {
            ensureRole(roleCode);
            Long roleId = jdbcTemplate.queryForObject("select id from sys_role where code = ?", Long.class, roleCode);
            Integer count = jdbcTemplate.queryForObject("""
                            select count(*)
                            from sys_user_role
                            where user_id = ? and role_id = ?
                            """,
                    Integer.class, userId, roleId);
            if (count == null || count == 0) {
                jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
            }
        }
    }

    private void ensureRole(String roleCode) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from sys_role where code = ?", Integer.class, roleCode);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_role (code, name) values (?, ?)", roleCode, roleCode);
        }
    }

    private void addPermissions(String roleCode, List<String> permissionCodes) {
        ensureRole(roleCode);
        Long roleId = jdbcTemplate.queryForObject("select id from sys_role where code = ?", Long.class, roleCode);
        for (String permissionCode : permissionCodes) {
            Integer permissionCount = jdbcTemplate.queryForObject("select count(*) from sys_permission where code = ?", Integer.class, permissionCode);
            if (permissionCount == null || permissionCount == 0) {
                jdbcTemplate.update("insert into sys_permission (code, name, description) values (?, ?, ?)",
                        permissionCode, permissionCode, permissionCode);
            }
            Long permissionId = jdbcTemplate.queryForObject("select id from sys_permission where code = ?", Long.class, permissionCode);
            Integer linkCount = jdbcTemplate.queryForObject("""
                            select count(*)
                            from sys_role_permission
                            where role_id = ? and permission_id = ?
                            """,
                    Integer.class, roleId, permissionId);
            if (linkCount == null || linkCount == 0) {
                jdbcTemplate.update("insert into sys_role_permission (role_id, permission_id) values (?, ?)", roleId, permissionId);
            }
        }
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
