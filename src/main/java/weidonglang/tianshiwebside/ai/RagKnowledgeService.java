package weidonglang.tianshiwebside.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RagKnowledgeService {
    private static final List<AiSourceDocument> STATIC_RULES = List.of(
            new AiSourceDocument("rule:selection", "选课与退课规则", "RULE",
                    "学生只能在选课开放时间内选课。教学班容量已满时不能继续选课。每名学生同一教学班只能成功选择一次。退课后系统会释放教学班剩余容量。", 0),
            new AiSourceDocument("rule:status-change", "学籍异动申请规则", "RULE",
                    "学籍异动需要学生填写异动类型和原因，必要时上传证明材料。管理员审核通过后会更新学生学籍状态，并向学生发送通知。", 0),
            new AiSourceDocument("rule:registration", "报名申请规则", "RULE",
                    "微专业、重修、学分替代、成绩加分、分流确认和方向确认都通过报名申请入口提交。管理员审核后，学生可以在申请记录中查看状态和审核意见。", 0),
            new AiSourceDocument("rule:grade-exam", "成绩与考试规则", "RULE",
                    "成绩由管理员或教师维护，学生可以查询自己的成绩。考试安排关联教学班，学生选课后可以查看对应课程的考试时间、地点和考试类型。", 0),
            new AiSourceDocument("rule:evaluation", "教学评价规则", "RULE",
                    "学生对已选课程提交教学评价后，教师可以查看自己课程的评价统计，管理员可以查看全校评价汇总。", 0)
    );

    private final JdbcTemplate jdbcTemplate;

    public RagKnowledgeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AiSourceDocument> retrieve(String question, Principal principal) {
        List<AiSourceDocument> documents = new ArrayList<>();
        documents.addAll(STATIC_RULES);
        documents.addAll(loadRecentNotices());
        documents.addAll(loadTeachingPlans());
        if (principal != null) {
            documents.addAll(loadStudentSnapshot(principal.getName()));
        }
        return documents.stream()
                .map(document -> new AiSourceDocument(
                        document.id(),
                        document.title(),
                        document.type(),
                        trim(document.content(), 900),
                        score(question, document)
                ))
                .filter(document -> document.score() > 0 || document.type().equals("PROFILE"))
                .sorted(Comparator.comparingDouble(AiSourceDocument::score).reversed())
                .limit(8)
                .toList();
    }

    private List<AiSourceDocument> loadRecentNotices() {
        try {
            return jdbcTemplate.query("""
                            select id, title, content, category
                            from notice
                            order by pinned desc, published_at desc
                            limit 20
                            """,
                    (rs, rowNum) -> new AiSourceDocument(
                            "notice:" + rs.getLong("id"),
                            rs.getString("title"),
                            "NOTICE",
                            "[" + rs.getString("category") + "] " + rs.getString("content"),
                            0
                    ));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<AiSourceDocument> loadTeachingPlans() {
        try {
            return jdbcTemplate.query("""
                            select grade, major, term, course_code, course_name, credit, course_type, assessment_type
                            from teaching_plan_item
                            order by grade desc, major, term
                            limit 60
                            """,
                    (rs, rowNum) -> new AiSourceDocument(
                            "plan:" + rs.getString("grade") + ":" + rs.getString("major") + ":" + rs.getString("course_code"),
                            rs.getString("major") + " " + rs.getString("term") + " " + rs.getString("course_name"),
                            "TEACHING_PLAN",
                            "年级：" + rs.getString("grade")
                                    + "，专业：" + rs.getString("major")
                                    + "，学期：" + rs.getString("term")
                                    + "，课程：" + rs.getString("course_code") + " " + rs.getString("course_name")
                                    + "，学分：" + rs.getInt("credit")
                                    + "，类型：" + rs.getString("course_type")
                                    + "，考核：" + rs.getString("assessment_type"),
                            0
                    ));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<AiSourceDocument> loadStudentSnapshot(String username) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                            select s.student_no, s.college, s.major, s.class_name, s.grade, s.status,
                                   coalesce(sum(case when ag.score >= 60 then c.credit else 0 end), 0) as passed_credit,
                                   coalesce(sum(case when ag.score < 60 then 1 else 0 end), 0) as failed_count
                            from sys_user u
                            join student s on s.user_id = u.id
                            left join academic_grade ag on ag.student_id = s.id
                            left join course c on c.id = ag.course_id
                            where u.username = ?
                            group by s.id
                            """,
                    username);
            if (rows.isEmpty()) {
                return List.of();
            }
            Map<String, Object> row = rows.get(0);
            String content = "学生：" + row.get("student_no")
                    + "，学院：" + row.get("college")
                    + "，专业：" + row.get("major")
                    + "，班级：" + row.get("class_name")
                    + "，年级：" + row.get("grade")
                    + "，学籍状态：" + row.get("status")
                    + "，已通过学分：" + row.get("passed_credit")
                    + "，未通过课程数：" + row.get("failed_count");
            return List.of(new AiSourceDocument("profile:" + username, "当前学生学业概况", "PROFILE", content, 0.5));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private double score(String question, AiSourceDocument document) {
        String normalizedQuestion = normalize(question);
        String text = normalize(document.title() + " " + document.content());
        double score = 0;
        for (String token : tokens(normalizedQuestion)) {
            if (token.length() >= 2 && text.contains(token)) {
                score += Math.min(token.length(), 6);
            }
        }
        if (text.contains(normalizedQuestion) || normalizedQuestion.contains(normalize(document.title()))) {
            score += 10;
        }
        return score;
    }

    private List<String> tokens(String value) {
        List<String> tokens = new ArrayList<>();
        for (String part : value.split("[\\s,，。；;：:、]+")) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        String[] keywords = {"毕业", "学分", "选课", "退课", "成绩", "考试", "学籍", "异动", "重修", "申请", "评价", "通知", "审核"};
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                tokens.add(keyword);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
