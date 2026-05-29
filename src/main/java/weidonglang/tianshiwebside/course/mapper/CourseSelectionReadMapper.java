package weidonglang.tianshiwebside.course.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseSelectionReadMapper {
    /**
     * 功能：查询当前学期可选教学班列表。
     * 说明：关联课程、教学班和当前学生选课记录，返回容量、已选人数、选课时间窗口、
     * 教室等信息，供前端自主选课页面分页展示。
     */
    @Select("""
            select
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              c.credit as credit,
              c.category as category,
              co.teacher_name as teacher_name,
              co.term as term,
              co.capacity as capacity,
              (
                select count(*)
                from course_selection cs
                where cs.offering_id = co.id
              ) as selected_count,
              co.schedule_text as schedule_text,
              co.classroom as classroom,
              co.selection_start_at as selection_start_at,
              co.selection_end_at as selection_end_at,
              case when exists (
                select 1
                from course_selection cs
                join student s on s.id = cs.student_id
                join sys_user u on u.id = s.user_id
                where cs.offering_id = co.id
                  and u.username = #{username}
              ) then true else false end as selected
            from course_offering co
            join course c on c.id = co.course_id
            where co.term = #{term}
            order by c.code asc
            limit #{size} offset #{offset}
            """)
    List<CourseOfferingRow> findOfferings(@Param("username") String username, @Param("term") String term,
                                          @Param("size") int size, @Param("offset") int offset);

    /**
     * 功能：统计当前学期教学班总数。
     * 说明：配合 findOfferings 实现前端分页，避免一次性加载全部教学班造成页面卡顿。
     */
    @Select("""
            select count(*)
            from course_offering co
            where co.term = #{term}
            """)
    long countOfferings(@Param("term") String term);

    /**
     * 功能：查询学生已选课程列表。
     * 说明：根据登录账号关联学生和选课记录，返回课程名称、教师、时间和教室，
     * 供“已选课程”区域分页展示。
     */
    @Select("""
            select
              cs.id as selection_id,
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              c.credit as credit,
              co.teacher_name as teacher_name,
              co.schedule_text as schedule_text,
              co.classroom as classroom,
              cs.selected_at as selected_at
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            join course_offering co on co.id = cs.offering_id
            join course c on c.id = co.course_id
            where u.username = #{username}
            order by cs.selected_at desc
            limit #{size} offset #{offset}
            """)
    List<CourseSelectionRow> findSelectedCourses(@Param("username") String username,
                                                @Param("size") int size, @Param("offset") int offset);

    /**
     * 功能：统计学生已选课程数量。
     * 说明：用于已选课程分页组件计算总页数。
     */
    @Select("""
            select count(*)
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            """)
    long countSelectedCourses(@Param("username") String username);
}
