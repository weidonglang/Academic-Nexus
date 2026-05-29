package weidonglang.tianshiwebside.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DashboardMapper {
    @Select("""
            select count(*)
            from course_offering
            where term = #{term}
            """)
    int countTermOfferings(@Param("term") String term);

    @Select("""
            select count(*)
            from exam_schedule es
            join course_offering co on co.id = es.course_offering_id
            join student s on s.student_no = #{username}
            where s.grade is not null
            """)
    int countUpcomingExams(@Param("username") String username);

    @Select("""
            select coalesce(sum(c.credit), 0)
            from academic_grade ag
            join student s on s.id = ag.student_id
            join sys_user u on u.id = s.user_id
            join course c on c.id = ag.course_id
            where u.username = #{username}
            """)
    int sumEarnedCredits(@Param("username") String username);

    @Select("""
            select count(*)
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            left join teaching_evaluation te on te.student_id = s.id and te.offering_id = cs.offering_id
            where u.username = #{username}
              and te.id is null
            """)
    int countPendingEvaluations(@Param("username") String username);

    @Select("""
            select
              '考试' as type,
              concat(c.name, '期末考试') as title,
              es.exam_time as event_time
            from exam_schedule es
            join course_offering co on co.id = es.course_offering_id
            join course c on c.id = co.course_id
            join student s on s.student_no = #{username}
            where s.grade is not null
            order by es.exam_time asc
            limit 5
            """)
    List<DashboardEventRow> findRecentEvents(@Param("username") String username);
}
