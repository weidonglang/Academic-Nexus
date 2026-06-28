package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CourseSelectionTermConsistencyTests extends HttpRegressionTestSupport {
    @Test
    void courseSelectionUsesRequestedTermAndCurrentTermEndpoint() throws Exception {
        String suffix = suffix();
        String student = "v141_term_student_" + suffix;
        String term = "2030-2031-1";
        seedStudent(student, "学期学生");
        jdbcTemplate.update("insert into course (code, name, credit, category) values (?, ?, ?, ?)",
                "TERM" + suffix, "学期切换课程", 2, "专业选修");
        Long courseId = jdbcTemplate.queryForObject("select id from course where code = ?", Long.class, "TERM" + suffix);
        jdbcTemplate.update("""
                        insert into course_offering
                          (course_id, teacher_name, term, capacity, schedule_text, classroom, selection_start_at, selection_end_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                courseId, "学期老师", term, 30, "周一 1-2节", "学期楼 101",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));

        String token = login(student);
        JsonNode currentTerm = json(get("/api/academic/current-term", token), HttpStatus.OK);
        assertThat(currentTerm.at("/data/term").asText()).isNotBlank();

        JsonNode offerings = json(get("/api/course-selection/offerings?term=" + term + "&page=1&size=10", token), HttpStatus.OK);
        assertThat(offerings.at("/data/records").toString()).contains("TERM" + suffix, term);
    }
}
