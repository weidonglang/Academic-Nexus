package weidonglang.tianshiwebside.student.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import weidonglang.tianshiwebside.student.ApplicationStatus;

import java.time.Instant;
import java.util.List;

@Mapper
public interface AdminStatusChangeMapper {
    @Select("""
            select
              a.id as id,
              s.id as student_id,
              s.student_no as student_no,
              u.display_name as student_name,
              s.college as college,
              s.major as major,
              s.class_name as class_name,
              s.status as student_status,
              a.type as type,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_status_change_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where (#{status} is null or a.status = #{status})
              and (
                #{keyword} is null
                or s.student_no like #{keyword}
                or u.display_name like #{keyword}
                or s.major like #{keyword}
                or s.class_name like #{keyword}
              )
            order by a.submitted_at desc
            limit #{size} offset #{offset}
            """)
    List<AdminStatusChangeRow> findApplications(
            @Param("status") ApplicationStatus status,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Select("""
            select count(*)
            from student_status_change_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where (#{status} is null or a.status = #{status})
              and (
                #{keyword} is null
                or s.student_no like #{keyword}
                or u.display_name like #{keyword}
                or s.major like #{keyword}
                or s.class_name like #{keyword}
              )
            """)
    long countApplications(
            @Param("status") ApplicationStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            select
              a.id as id,
              s.id as student_id,
              s.student_no as student_no,
              u.display_name as student_name,
              s.college as college,
              s.major as major,
              s.class_name as class_name,
              s.status as student_status,
              a.type as type,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_status_change_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where a.id = #{id}
            """)
    AdminStatusChangeRow findApplicationById(@Param("id") Long id);

    @Update("""
            update student_status_change_application
            set status = #{status},
                reviewed_at = #{reviewedAt},
                review_comment = #{reviewComment}
            where id = #{id}
            """)
    int updateReviewResult(
            @Param("id") Long id,
            @Param("status") ApplicationStatus status,
            @Param("reviewedAt") Instant reviewedAt,
            @Param("reviewComment") String reviewComment
    );

    @Update("""
            update student
            set status = #{status}
            where id = #{studentId}
            """)
    int updateStudentStatus(@Param("studentId") Long studentId, @Param("status") String status);
}
