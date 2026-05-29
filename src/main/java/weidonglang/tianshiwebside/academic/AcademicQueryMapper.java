package weidonglang.tianshiwebside.academic;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AcademicQueryMapper {
    @Select("""
            select
              ag.term as term,
              c.code as course_code,
              c.name as course_name,
              c.credit as credit,
              c.category as course_type,
              ag.score as score,
              ag.grade_point as grade_point,
              ag.exam_type as exam_type
            from academic_grade ag
            join student s on s.id = ag.student_id
            join sys_user u on u.id = s.user_id
            join course c on c.id = ag.course_id
            where u.username = #{username}
            order by ag.term desc, c.code asc
            """)
    List<GradeRecordRow> findGradesByUsername(@Param("username") String username);

    @Select("""
            select
              co.term as term,
              c.code as course_code,
              c.name as course_name,
              es.exam_time as exam_time,
              es.room as room,
              es.seat_no as seat_no,
              es.exam_type as exam_type,
              es.status as status
            from exam_schedule es
            join course_offering co on co.id = es.course_offering_id
            join course c on c.id = co.course_id
            join course_selection cs on cs.offering_id = co.id
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by es.exam_time asc
            """)
    List<ExamScheduleRow> findExamsByUsername(@Param("username") String username);

    @Select("""
            <script>
            select
              campus as campus,
              building as building,
              room as room,
              capacity as capacity,
              room_type as room_type,
              available_slot as available_slot
            from classroom
            where (#{campus} is null or #{campus} = '' or campus = #{campus})
              and (#{building} is null or #{building} = '' or building = #{building})
              and (#{slot} is null or #{slot} = '' or available_slot = #{slot})
            order by campus asc, building asc, room asc
            </script>
            """)
    List<FreeClassroomRow> findFreeClassrooms(
            @Param("campus") String campus,
            @Param("building") String building,
            @Param("slot") String slot
    );
}
