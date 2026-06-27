package weidonglang.tianshiwebside;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CourseGrabValidationRegressionTests extends HttpRegressionTestSupport {
    @Test
    void missingOfferingIdReturnsBadRequest() throws Exception {
        String username = "v141_grab_missing_" + suffix();
        seedStudent(username, "抢课校验学生");

        assertThat(post("/api/course-selection/grab", login(username), """
                {"requestId":"missing-offering"}
                """).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
