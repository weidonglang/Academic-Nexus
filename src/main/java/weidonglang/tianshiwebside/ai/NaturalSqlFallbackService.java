package weidonglang.tianshiwebside.ai;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NaturalSqlFallbackService {
    public NaturalSqlGenerateResponse generate(String question, List<String> allowedTables) {
        String q = question == null ? "" : question;
        String sql;
        String explanation;

        if (q.contains("选课人数") || q.contains("最多") || q.contains("热门")) {
            sql = """
                    select c.name as course_name, co.teacher_name, co.term, co.capacity,
                           count(cs.id) as selected_count
                    from course_offering co
                    join course c on c.id = co.course_id
                    left join course_selection cs on cs.offering_id = co.id
                    group by co.id, c.name, co.teacher_name, co.term, co.capacity
                    order by selected_count desc
                    limit 10
                    """;
            explanation = "按教学班统计已选人数，展示选课人数最多的课程。";
        } else if (q.contains("挂科") || q.contains("不及格") || q.contains("低于60")) {
            sql = """
                    select c.name as course_name, count(*) as failed_count
                    from academic_grade ag
                    join course c on c.id = ag.course_id
                    where ag.score < 60
                    group by c.id, c.name
                    order by failed_count desc
                    limit 10
                    """;
            explanation = "统计成绩低于 60 分的课程挂科人数。";
        } else if (q.contains("容量") || q.contains("90%") || q.contains("爆满")) {
            sql = """
                    select c.name as course_name, co.teacher_name, co.capacity,
                           count(cs.id) as selected_count,
                           round(count(cs.id) * 100.0 / nullif(co.capacity, 0), 2) as usage_rate
                    from course_offering co
                    join course c on c.id = co.course_id
                    left join course_selection cs on cs.offering_id = co.id
                    group by co.id, c.name, co.teacher_name, co.capacity
                    having usage_rate >= 90
                    order by usage_rate desc
                    limit 20
                    """;
            explanation = "统计容量使用率超过 90% 的教学班。";
        } else if (q.contains("没录成绩") || q.contains("未录成绩")) {
            sql = """
                    select c.name as course_name, co.teacher_name, co.term
                    from course_offering co
                    join course c on c.id = co.course_id
                    left join academic_grade ag on ag.course_id = c.id and ag.term = co.term
                    group by co.id, c.name, co.teacher_name, co.term
                    having count(ag.id) = 0
                    order by co.term desc
                    limit 20
                    """;
            explanation = "查找当前没有任何成绩记录的教学班。";
        } else if (q.contains("公告") || q.contains("通知")) {
            sql = """
                    select title, category, publisher, published_at
                    from notice
                    order by published_at desc
                    limit 20
                    """;
            explanation = "查询最近发布的通知公告。";
        } else {
            sql = """
                    select c.name as course_name, co.teacher_name, co.term, co.capacity,
                           count(cs.id) as selected_count
                    from course_offering co
                    join course c on c.id = co.course_id
                    left join course_selection cs on cs.offering_id = co.id
                    group by co.id, c.name, co.teacher_name, co.term, co.capacity
                    order by selected_count desc
                    limit 10
                    """;
            explanation = "未识别到明确查询类型，默认生成课程选课人数排行。";
        }

        return new NaturalSqlGenerateResponse(
                sql.strip(),
                explanation,
                List.of("当前使用主系统兜底规则生成 SQL，接入 ai-service/Ollama 后可生成更灵活查询。"),
                allowedTables,
                "local-fallback"
        );
    }
}
