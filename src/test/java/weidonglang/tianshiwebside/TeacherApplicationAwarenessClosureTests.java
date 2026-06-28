package weidonglang.tianshiwebside;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TeacherApplicationAwarenessClosureTests extends HttpRegressionTestSupport {
    @Test
    void homeroomTeacherCanReadOwnClassApplicationsOnly() throws Exception {
        String suffix = suffix();
        String teacher = "aware_teacher_" + suffix;
        String otherTeacher = "aware_other_teacher_" + suffix;
        String student = "aware_student_" + suffix;
        String className = "教师感知班级-" + suffix;
        seedUser(teacher, "感知教师-" + suffix, List.of("TEACHER"), List.of());
        seedUser(otherTeacher, "无关教师-" + suffix, List.of("TEACHER"), List.of());
        seedStudent(student, "感知学生");
        Long teacherUserId = jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, teacher);
        jdbcTemplate.update("""
                        insert into academic_class
                          (college, major, grade, class_name, advisor, homeroom_teacher_user_id, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "信息工程学院", "软件工程", "2026", className, "感知教师", teacherUserId, Instant.now(), Instant.now());
        Long classId = jdbcTemplate.queryForObject("select id from academic_class where class_name = ?", Long.class, className);
        jdbcTemplate.update("update student set class_name = ? where student_no = ?", className, student);
        Long studentId = jdbcTemplate.queryForObject("select id from student where student_no = ?", Long.class, student);
        jdbcTemplate.update("""
                        insert into student_status_change_application
                          (student_id, type, reason, status, submitted_at)
                        values (?, ?, ?, ?, ?)
                        """, studentId, "OTHER", "教师只读感知", "SUBMITTED", Instant.now());

        JsonNode awareness = json(get("/api/teacher/application-awareness/classes/" + classId, login(teacher)), HttpStatus.OK);
        assertThat(awareness.at("/data/statusChanges").toString()).contains("教师只读感知", student);
        assertThat(get("/api/teacher/application-awareness/classes/" + classId, login(otherTeacher)).statusCode())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(get("/api/teacher/application-awareness/classes/" + classId, login(student)).statusCode())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void courseTeacherCanReadRelatedRegistrationSummaryWithoutAttachments() throws Exception {
        String suffix = suffix();
        String teacher = "aware_course_teacher_" + suffix;
        String otherTeacher = "aware_course_other_" + suffix;
        String student = "aware_course_student_" + suffix;
        seedUser(teacher, "课程感知教师-" + suffix, List.of("TEACHER"), List.of());
        seedUser(otherTeacher, "无关课程教师-" + suffix, List.of("TEACHER"), List.of());
        seedStudent(student, "课程感知学生");
        jdbcTemplate.update("insert into course (code, name, credit, category) values (?, ?, ?, ?)",
                "AWARE" + suffix.toUpperCase(), "教师感知课程-" + suffix, 2, "专业课");
        Long courseId = jdbcTemplate.queryForObject("select id from course where code = ?", Long.class, "AWARE" + suffix.toUpperCase());
        jdbcTemplate.update("""
                        insert into course_offering
                          (course_id, teacher_name, term, capacity, schedule_text, classroom, selection_start_at, selection_end_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                courseId, "课程感知教师-" + suffix, "2031-2032-1", 30, "周二 3-4节", "B201",
                Instant.parse("2031-08-20T00:00:00Z"), Instant.parse("2031-09-30T23:59:59Z"));
        Long studentId = jdbcTemplate.queryForObject("select id from student where student_no = ?", Long.class, student);
        jdbcTemplate.update("""
                        insert into student_registration_application
                          (student_id, type, target_name, course_name, reason, status, submitted_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                studentId, "RETAKE_REGISTRATION", "教师感知课程-" + suffix, "教师感知课程-" + suffix,
                "教师只读报名概要", "SUBMITTED", Instant.now());

        JsonNode rows = json(get("/api/teacher/application-awareness/registrations", login(teacher)), HttpStatus.OK);
        assertThat(rows.at("/data").toString()).contains("教师感知课程-" + suffix, student);
        assertThat(rows.at("/data").toString()).doesNotContain("attachment");

        JsonNode unrelated = json(get("/api/teacher/application-awareness/registrations", login(otherTeacher)), HttpStatus.OK);
        assertThat(unrelated.at("/data").toString()).doesNotContain("教师感知课程-" + suffix);
    }
}
