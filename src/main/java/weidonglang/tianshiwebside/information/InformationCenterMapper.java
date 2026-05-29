package weidonglang.tianshiwebside.information;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;

@Mapper
public interface InformationCenterMapper {
    @Select("""
            select
              aw.term as term,
              aw.level as level,
              aw.reason as reason,
              aw.status as status,
              aw.created_at as created_at
            from academic_warning aw
            join student s on s.id = aw.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by aw.created_at desc
            """)
    List<AcademicWarningRow> findWarningsByUsername(@Param("username") String username);

    @Select("""
            select
              ga.audit_item as audit_item,
              ga.required_value as required_value,
              ga.current_value as current_value,
              ga.passed as passed,
              ga.remark as remark,
              ga.updated_at as updated_at
            from graduation_audit ga
            join student s on s.id = ga.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by ga.id asc
            """)
    List<GraduationAuditRow> findGraduationAuditByUsername(@Param("username") String username);

    @Select("""
            select
              co.term as term,
              s.class_name as class_name,
              c.code as course_code,
              c.name as course_name,
              c.credit as credit,
              c.category as course_type,
              co.teacher_name as teacher_name,
              co.schedule_text as schedule_text,
              co.classroom as classroom
            from course_selection cs
            join student s on s.id = cs.student_id
            join course_offering co on co.id = cs.offering_id
            join course c on c.id = co.course_id
            where (#{className} is null or #{className} = '' or s.class_name = #{className})
              and (#{term} is null or #{term} = '' or co.term = #{term})
            group by co.term, s.class_name, c.code, c.name, c.credit, c.category, co.teacher_name, co.schedule_text, co.classroom
            order by co.term desc, c.code asc
            """)
    List<ClassScheduleRow> findClassSchedule(@Param("className") String className, @Param("term") String term);

    @Select("""
            select
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              co.term as term,
              co.teacher_name as teacher_name,
              co.schedule_text as schedule_text,
              co.classroom as classroom,
              s.student_no as student_no,
              u.display_name as student_name,
              s.college as college,
              s.major as major,
              s.class_name as class_name,
              cs.selected_at as selected_at
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            join course_offering co on co.id = cs.offering_id
            join course c on c.id = co.course_id
            where (#{offeringId} is null or co.id = #{offeringId})
              and (#{term} is null or #{term} = '' or co.term = #{term})
            order by co.term desc, c.code asc, s.student_no asc
            """)
    List<CourseRosterRow> findCourseRoster(@Param("offeringId") Long offeringId, @Param("term") String term);

    @Select("""
            select
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              co.term as term,
              co.teacher_name as teacher_name,
              co.schedule_text as schedule_text,
              co.classroom as classroom
            from course_offering co
            join course c on c.id = co.course_id
            where (#{term} is null or #{term} = '' or co.term = #{term})
            order by co.term desc, c.code asc
            """)
    List<OfferingOptionRow> findOfferingOptions(@Param("term") String term);

    @Select("""
            select
              c.category as course_type,
              count(*) as course_count,
              coalesce(sum(c.credit), 0) as total_credits,
              coalesce(sum(case when ag.score >= 60 then c.credit else 0 end), 0) as passed_credits,
              coalesce(avg(ag.score), 0) as average_score
            from academic_grade ag
            join student s on s.id = ag.student_id
            join sys_user u on u.id = s.user_id
            join course c on c.id = ag.course_id
            where u.username = #{username}
            group by c.category
            order by c.category asc
            """)
    List<AcademicProgressRow> findAcademicProgressByUsername(@Param("username") String username);

    @Select("""
            select
              term as term,
              course_code as course_code,
              course_name as course_name,
              credit as credit,
              course_type as course_type,
              assessment_type as assessment_type
            from teaching_plan_item
            where (#{major} is null or #{major} = '' or major = #{major})
              and (#{grade} is null or #{grade} = '' or grade = #{grade})
            order by term asc, course_code asc
            """)
    List<TeachingPlanRow> findTeachingPlan(@Param("major") String major, @Param("grade") String grade);

    @Select("""
            select
              co.term as term,
              c.code as course_code,
              c.name as course_name,
              c.credit as credit,
              co.teacher_name as teacher_name,
              co.schedule_text as schedule_text,
              co.classroom as classroom,
              #{week} as week
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            join course_offering co on co.id = cs.offering_id
            join course c on c.id = co.course_id
            where u.username = #{username}
            order by co.term desc, c.code asc
            """)
    List<WeeklyScheduleRow> findWeeklySchedule(@Param("username") String username, @Param("week") Integer week);

    @Select("""
            select
              tg.title as title,
              tg.advisor as advisor,
              tg.proposal_score as proposal_score,
              tg.midterm_score as midterm_score,
              tg.defense_score as defense_score,
              tg.final_score as final_score,
              tg.grade_level as grade_level,
              tg.status as status,
              tg.updated_at as updated_at
            from thesis_grade tg
            join student s on s.id = tg.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by tg.updated_at desc
            """)
    List<ThesisGradeRow> findThesisGradesByUsername(@Param("username") String username);

    @Select("""
            select
              tf.id as id,
              tf.category as category,
              tf.title as title,
              tf.content as content,
              tf.status as status,
              tf.reply as reply,
              tf.submitted_at as submitted_at,
              tf.replied_at as replied_at
            from teaching_feedback tf
            join student s on s.id = tf.student_id
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            order by tf.submitted_at desc
            """)
    List<TeachingFeedbackRow> findFeedbackByUsername(@Param("username") String username);

    @Insert("""
            insert into teaching_feedback
              (student_id, category, title, content, status, submitted_at)
            values
              (#{studentId}, #{category}, #{title}, #{content}, #{status}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFeedback(InsertFeedbackCommand command);

    record AcademicWarningRow(String term, String level, String reason, String status, Instant createdAt) {
    }

    record GraduationAuditRow(String auditItem, String requiredValue, String currentValue, Boolean passed, String remark, Instant updatedAt) {
    }

    record ClassScheduleRow(String term, String className, String courseCode, String courseName, Integer credit,
                            String courseType, String teacherName, String scheduleText, String classroom) {
    }

    record CourseRosterRow(Long offeringId, String courseCode, String courseName, String term, String teacherName,
                           String scheduleText, String classroom, String studentNo, String studentName,
                           String college, String major, String className, Instant selectedAt) {
    }

    record OfferingOptionRow(Long offeringId, String courseCode, String courseName, String term, String teacherName,
                             String scheduleText, String classroom) {
    }

    record AcademicProgressRow(String courseType, Integer courseCount, Integer totalCredits, Integer passedCredits,
                               Double averageScore) {
    }

    record TeachingPlanRow(String term, String courseCode, String courseName, Integer credit, String courseType,
                           String assessmentType) {
    }

    record WeeklyScheduleRow(String term, String courseCode, String courseName, Integer credit, String teacherName,
                             String scheduleText, String classroom, Integer week) {
    }

    record ThesisGradeRow(String title, String advisor, Integer proposalScore, Integer midtermScore,
                          Integer defenseScore, Integer finalScore, String gradeLevel, String status, Instant updatedAt) {
    }

    record TeachingFeedbackRow(Long id, String category, String title, String content, String status, String reply,
                               Instant submittedAt, Instant repliedAt) {
    }

    final class InsertFeedbackCommand {
        private Long id;
        private final Long studentId;
        private final String category;
        private final String title;
        private final String content;
        private final String status;
        private final Instant submittedAt;

        public InsertFeedbackCommand(Long studentId, String category, String title, String content, String status, Instant submittedAt) {
            this.studentId = studentId;
            this.category = category;
            this.title = title;
            this.content = content;
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

        public String getCategory() {
            return category;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getStatus() {
            return status;
        }

        public Instant getSubmittedAt() {
            return submittedAt;
        }
    }
}
