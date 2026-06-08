package weidonglang.tianshiwebside.student.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import weidonglang.tianshiwebside.student.ApplicationStatus;
import weidonglang.tianshiwebside.student.StatusChangeType;

import java.time.Instant;
import java.util.List;

@Mapper
public interface StudentMapper {
    @Select("""
            select
              s.id as student_id,
              s.student_no as student_no,
              u.display_name as name,
              s.college as college,
              s.major as major,
              s.class_name as class_name,
              s.grade as grade,
              s.status as status,
              s.phone as phone,
              s.email as email,
              s.address as address
            from student s
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            """)
    StudentProfileRow findProfileByUsername(@Param("username") String username);

    @Select("""
            select s.id
            from student s
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            """)
    Long findStudentIdByUsername(@Param("username") String username);

    @Update("""
            update student s
            set phone = #{phone},
                email = #{email},
                address = #{address}
            where s.user_id = (
              select u.id
              from sys_user u
              where u.username = #{username}
            )
            """)
    int updateContactByUsername(
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("address") String address
    );

    @Select("""
            select
              a.id as id,
              a.type as type,
              a.reason as reason,
              a.status as status,
              a.submitted_at as submitted_at,
              a.reviewed_at as reviewed_at,
              a.review_comment as review_comment
            from student_status_change_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by a.submitted_at desc
            limit #{size} offset #{offset}
            """)
    List<StatusChangeApplicationRow> findStatusChangesByUsername(@Param("username") String username,
                                                                 @Param("size") int size,
                                                                 @Param("offset") int offset);

    @Select("""
            select count(*)
            from student_status_change_application a
            join student s on s.id = a.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            """)
    long countStatusChangesByUsername(@Param("username") String username);

    @Insert("""
            insert into student_status_change_application
              (student_id, type, reason, status, submitted_at)
            values
              (#{studentId}, #{type}, #{reason}, #{status}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStatusChange(InsertStatusChangeCommand command);

    record StudentProfileRow(
            Long studentId,
            String studentNo,
            String name,
            String college,
            String major,
            String className,
            String grade,
            String status,
            String phone,
            String email,
            String address
    ) {
    }

    record StatusChangeApplicationRow(
            Long id,
            StatusChangeType type,
            String reason,
            ApplicationStatus status,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewComment
    ) {
    }

    final class InsertStatusChangeCommand {
        private Long id;
        private final Long studentId;
        private final StatusChangeType type;
        private final String reason;
        private final ApplicationStatus status;
        private final Instant submittedAt;

        public InsertStatusChangeCommand(Long studentId, StatusChangeType type, String reason, ApplicationStatus status, Instant submittedAt) {
            this.studentId = studentId;
            this.type = type;
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

        public StatusChangeType getType() {
            return type;
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
