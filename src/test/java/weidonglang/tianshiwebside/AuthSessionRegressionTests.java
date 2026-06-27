package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSessionRegressionTests extends HttpRegressionTestSupport {
    @Test
    void refreshRotatesTokenLogoutRevokesAndDisabledUserCannotKeepUsingToken() throws Exception {
        String suffix = suffix();
        String username = "v141_auth_" + suffix;
        seedStudent(username, "认证学生");

        JsonNode login = json(post("/api/auth/login", null, """
                {"username":"%s","password":"123456"}
                """.formatted(username)), HttpStatus.OK);
        String accessToken = login.at("/data/accessToken").asText();
        String refreshToken = login.at("/data/refreshToken").asText();
        assertThat(get("/api/me", accessToken).statusCode()).isEqualTo(HttpStatus.OK.value());

        JsonNode refresh = json(post("/api/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(refreshToken)), HttpStatus.OK);
        String refreshedAccessToken = refresh.at("/data/accessToken").asText();
        String rotatedRefreshToken = refresh.at("/data/refreshToken").asText();
        assertThat(refreshedAccessToken).isNotEqualTo(accessToken);
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);
        assertThat(post("/api/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(refreshToken)).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        json(post("/api/auth/logout", refreshedAccessToken, """
                {"refreshToken":"%s"}
                """.formatted(rotatedRefreshToken)), HttpStatus.OK);
        assertThat(get("/api/me", refreshedAccessToken).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(post("/api/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(rotatedRefreshToken)).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        JsonNode secondLogin = json(post("/api/auth/login", null, """
                {"username":"%s","password":"123456"}
                """.formatted(username)), HttpStatus.OK);
        String disabledAccess = secondLogin.at("/data/accessToken").asText();
        String disabledRefresh = secondLogin.at("/data/refreshToken").asText();
        jdbcTemplate.update("update sys_user set status = 'DISABLED' where username = ?", username);

        assertThat(get("/api/me", disabledAccess).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(post("/api/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(disabledRefresh)).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
