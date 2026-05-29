package weidonglang.tianshiwebside.student.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import weidonglang.tianshiwebside.student.ApplicationStatus;
import weidonglang.tianshiwebside.student.RegistrationApplicationType;

import java.time.Instant;
import java.util.List;

@Mapper
public interface RegistrationApplicationMapper {
    @Select("""
            select
              a.id as id,
              a.type as type,
              a.target_name as target_name,
              a.course_name as course_name,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_registration_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
              and (#{type} is null or a.type = #{type})
            order by a.submitted_at desc
            """)
    List<RegistrationApplicationRow> findMine(@Param("username") String username, @Param("type") RegistrationApplicationType type);

    @Select("""
            select
              a.id as id,
              s.id as student_id,
              s.student_no as student_no,
              u.display_name as student_name,
              s.college as college,
              s.major as major,
              s.class_name as class_name,
              a.type as type,
              a.target_name as target_name,
              a.course_name as course_name,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_registration_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where (#{status} is null or a.status = #{status})
              and (#{type} is null or a.type = #{type})
              and (
                #{keyword} is null
                or s.student_no like #{keyword}
                or u.display_name like #{keyword}
                or a.target_name like #{keyword}
                or a.course_name like #{keyword}
              )
            order by a.submitted_at desc
            """)
    List<AdminRegistrationApplicationRow> findAdminApplications(
            @Param("status") ApplicationStatus status,
            @Param("type") RegistrationApplicationType type,
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
              a.type as type,
              a.target_name as target_name,
              a.course_name as course_name,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_registration_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where a.id = #{id}
            """)
    AdminRegistrationApplicationRow findAdminApplicationById(@Param("id") Long id);

    @Insert("""
            insert into student_registration_application
              (student_id, type, target_name, course_name, reason, status, submitted_at)
            values
              (#{studentId}, #{type}, #{targetName}, #{courseName}, #{reason}, #{status}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InsertRegistrationApplicationCommand command);

    @Update("""
            update student_registration_application
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

    record RegistrationApplicationRow(
            Long id,
            RegistrationApplicationType type,
            String targetName,
            String courseName,
            String reason,
            ApplicationStatus status,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewComment
    ) {
    }

    record AdminRegistrationApplicationRow(
            Long id,
            Long studentId,
            String studentNo,
            String studentName,
            String college,
            String major,
            String className,
            RegistrationApplicationType type,
            String targetName,
            String courseName,
            String reason,
            ApplicationStatus status,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewComment
    ) {
    }

    final class InsertRegistrationApplicationCommand {
        private Long id;
        private final Long studentId;
        private final RegistrationApplicationType type;
        private final String targetName;
        private final String courseName;
        private final String reason;
        private final ApplicationStatus status;
        private final Instant submittedAt;

        public InsertRegistrationApplicationCommand(Long studentId, RegistrationApplicationType type, String targetName, String courseName, String reason, ApplicationStatus status, Instant submittedAt) {
            this.studentId = studentId;
            this.type = type;
            this.targetName = targetName;
            this.courseName = courseName;
            this.reason = reason;
            this.status = status;
            this.submittedAt = submittedAt;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getStudentId() {
            return studentId;
        }

        public RegistrationApplicationType getType() {
            return type;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getCourseName() {
            return courseName;
        }

        public String getReason() {
            return reason;
        }

        public ApplicationStatus getStatus() {
            return status;
        }

        public Instant getSubmittedAt() {
            return submittedAt;
        }
    }
}
