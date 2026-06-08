package weidonglang.tianshiwebside.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
public class AcademicProfileService {
    private final JdbcTemplate jdbcTemplate;
    private final AiCallLogService callLogService;

    public AcademicProfileService(JdbcTemplate jdbcTemplate, AiCallLogService callLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.callLogService = callLogService;
    }

    public AcademicProfileResponse currentProfile(Principal principal) {
        long start = System.nanoTime();
        String username = principal == null ? "" : principal.getName();
        Map<String, Object> student = jdbcTemplate.queryForMap("""
                        select s.id, s.student_no, u.display_name, s.college, s.major, s.class_name, s.grade, s.status
                        from student s
                        join sys_user u on u.id = s.user_id
                        where u.username = ?
                        """,
                username
        );
        long studentId = ((Number) student.get("id")).longValue();
        int earnedCredits = intValue(jdbcTemplate.queryForObject("""
                        select coalesce(sum(case when ag.score >= 60 then c.credit else 0 end), 0)
                        from academic_grade ag
                        join course c on c.id = ag.course_id
                        where ag.student_id = ?
                        """,
                Number.class,
                studentId
        ));
        int plannedCredits = intValue(jdbcTemplate.queryForObject("""
                        select coalesce(sum(credit), 0)
                        from teaching_plan_item
                        where major = ? and grade = ?
                        """,
                Number.class,
                student.get("major"),
                student.get("grade")
        ));
        List<AcademicProfileResponse.CourseRiskRow> failedCourses = jdbcTemplate.query("""
                        select c.code, c.name, c.credit, ag.score, ag.term
                        from academic_grade ag
                        join course c on c.id = ag.course_id
                        where ag.student_id = ? and ag.score < 60
                        order by ag.term desc, c.code
                        """,
                (rs, rowNum) -> new AcademicProfileResponse.CourseRiskRow(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credit"),
                        rs.getInt("score"),
                        rs.getString("term")
                ),
                studentId
        );
        List<AcademicProfileResponse.ProgressRow> progress = jdbcTemplate.query("""
                        select c.category, count(*) as course_count,
                               coalesce(sum(c.credit), 0) as total_credits,
                               coalesce(sum(case when ag.score >= 60 then c.credit else 0 end), 0) as passed_credits,
                               coalesce(avg(ag.score), 0) as average_score
                        from academic_grade ag
                        join course c on c.id = ag.course_id
                        where ag.student_id = ?
                        group by c.category
                        order by c.category
                        """,
                (rs, rowNum) -> new AcademicProfileResponse.ProgressRow(
                        rs.getString("category"),
                        rs.getInt("course_count"),
                        rs.getInt("total_credits"),
                        rs.getInt("passed_credits"),
                        rs.getDouble("average_score")
                ),
                studentId
        );
        List<AcademicProfileResponse.AuditRow> audits = jdbcTemplate.query("""
                        select audit_item, required_value, current_value, passed, remark
                        from graduation_audit
                        where student_id = ?
                        order by id
                        """,
                (rs, rowNum) -> new AcademicProfileResponse.AuditRow(
                        rs.getString("audit_item"),
                        rs.getString("required_value"),
                        rs.getString("current_value"),
                        rs.getBoolean("passed"),
                        rs.getString("remark")
                ),
                studentId
        );
        int remainingCredits = Math.max(0, plannedCredits - earnedCredits);
        String risk = riskLevel(remainingCredits, failedCourses.size(), audits);
        String suggestion = suggestion(risk, remainingCredits, failedCourses);
        AcademicProfileResponse response = new AcademicProfileResponse(
                String.valueOf(student.get("student_no")),
                String.valueOf(student.get("display_name")),
                String.valueOf(student.get("college")),
                String.valueOf(student.get("major")),
                String.valueOf(student.get("class_name")),
                String.valueOf(student.get("grade")),
                String.valueOf(student.get("status")),
                earnedCredits,
                plannedCredits,
                remainingCredits,
                failedCourses.size(),
                failedCourses.size(),
                directionStatus(studentId),
                risk,
                suggestion,
                failedCourses,
                progress,
                audits,
                "local-analysis"
        );
        callLogService.record(principal, "ACADEMIC_PROFILE", "学业画像", "local-analysis", elapsedMillis(start), true, null);
        return response;
    }

    private String directionStatus(long studentId) {
        Integer count = jdbcTemplate.queryForObject("""
                        select count(*)
                        from student_registration_application
                        where student_id = ? and type = 'DIRECTION_CONFIRM' and status = 'APPROVED'
                        """,
                Integer.class,
                studentId
        );
        return count != null && count > 0 ? "已确认" : "未确认";
    }

    private String riskLevel(int remainingCredits, int failedCount, List<AcademicProfileResponse.AuditRow> audits) {
        boolean auditFailed = audits.stream().anyMatch(row -> !row.passed());
        if (remainingCredits > 30 || failedCount >= 3 || auditFailed) return "高风险";
        if (remainingCredits > 10 || failedCount > 0) return "中风险";
        return "低风险";
    }

    private String suggestion(String risk, int remainingCredits, List<AcademicProfileResponse.CourseRiskRow> failedCourses) {
        if ("高风险".equals(risk)) {
            return "当前毕业风险较高，建议优先处理未通过课程、补足培养方案学分，并及时联系辅导员或教务管理员确认毕业审核项。";
        }
        if ("中风险".equals(risk)) {
            return "当前存在一定毕业风险，建议优先重修未通过课程，并结合教学计划补选缺口学分。";
        }
        if (!failedCourses.isEmpty()) {
            return "整体风险较低，但仍需关注未通过课程并在后续学期完成补救。";
        }
        if (remainingCredits > 0) {
            return "整体进度正常，后续按教学计划继续修读剩余课程即可。";
        }
        return "当前学业进度良好，请继续关注考试安排、教学评价和毕业审核通知。";
    }

    private int intValue(Number number) {
        return number == null ? 0 : number.intValue();
    }

    private long elapsedMillis(long startNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
