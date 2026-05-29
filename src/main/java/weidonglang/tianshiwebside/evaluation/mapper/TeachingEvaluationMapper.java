package weidonglang.tianshiwebside.evaluation.mapper;

import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;

@Mapper
public interface TeachingEvaluationMapper {
    @Select("""
            select
              co.id as offering_id,
              cs.id as selection_id,
              c.code as course_code,
              c.name as course_name,
              co.teacher_name as teacher_name,
              co.term as term,
              co.schedule_text as schedule_text,
              co.classroom as classroom,
              case when te.id is null then false else true end as evaluated,
              te.teaching_score as teaching_score,
              te.content_score as content_score,
              te.interaction_score as interaction_score,
              te.overall_score as overall_score,
              te.comment as comment,
              te.submitted_at as submitted_at
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            join course_offering co on co.id = cs.offering_id
            join course c on c.id = co.course_id
            left join teaching_evaluation te on te.student_id = s.id and te.offering_id = co.id
            where u.username = #{username}
            order by co.term desc, c.code asc
            """)
    List<EvaluationTaskRow> findTasksByUsername(@Param("username") String username);

    @Select("""
            select s.id
            from student s
            join sys_user u on u.id = s.user_id
            where u.username = #{username}
            """)
    Long findStudentIdByUsername(@Param("username") String username);

    @Select("""
            select count(*)
            from course_selection
            where student_id = #{studentId}
              and offering_id = #{offeringId}
            """)
    int countSelection(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Select("""
            select count(*)
            from teaching_evaluation
            where student_id = #{studentId}
              and offering_id = #{offeringId}
            """)
    int countEvaluation(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Insert("""
            insert into teaching_evaluation (
              student_id,
              offering_id,
              teaching_score,
              content_score,
              interaction_score,
              overall_score,
              comment,
              submitted_at
            )
            values (
              #{studentId},
              #{offeringId},
              #{teachingScore},
              #{contentScore},
              #{interactionScore},
              #{overallScore},
              #{comment},
              #{submittedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEvaluation(InsertEvaluationCommand command);

    @Select("""
            select
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              co.teacher_name as teacher_name,
              co.term as term,
              (
                select count(*)
                from course_selection cs
                where cs.offering_id = co.id
              ) as selected_count,
              count(te.id) as submitted_count,
              avg(te.teaching_score) as average_teaching_score,
              avg(te.content_score) as average_content_score,
              avg(te.interaction_score) as average_interaction_score,
              avg(te.overall_score) as average_overall_score
            from course_offering co
            join course c on c.id = co.course_id
            left join teaching_evaluation te on te.offering_id = co.id
            where (#{term} is null or co.term = #{term})
            group by co.id, c.code, c.name, co.teacher_name, co.term
            order by co.term desc, c.code asc
            """)
    List<EvaluationSummaryRow> findSummaries(@Param("term") String term);

    @Select("""
            select
              te.id as evaluation_id,
              s.student_no as student_no,
              u.display_name as student_name,
              c.code as course_code,
              c.name as course_name,
              co.teacher_name as teacher_name,
              co.term as term,
              te.teaching_score as teaching_score,
              te.content_score as content_score,
              te.interaction_score as interaction_score,
              te.overall_score as overall_score,
              te.comment as comment,
              te.submitted_at as submitted_at
            from teaching_evaluation te
            join student s on s.id = te.student_id
            join sys_user u on u.id = s.user_id
            join course_offering co on co.id = te.offering_id
            join course c on c.id = co.course_id
            where (#{term} is null or co.term = #{term})
              and (#{offeringId} is null or co.id = #{offeringId})
            order by te.submitted_at desc
            """)
    List<EvaluationRecordRow> findRecords(@Param("term") String term, @Param("offeringId") Long offeringId);

    @Select("""
            select count(*)
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            left join teaching_evaluation te on te.student_id = s.id and te.offering_id = cs.offering_id
            where u.username = #{username}
              and te.id is null
            """)
    int countPendingTasks(@Param("username") String username);

    final class InsertEvaluationCommand {
        private Long id;
        private final Long studentId;
        private final Long offeringId;
        private final Integer teachingScore;
        private final Integer contentScore;
        private final Integer interactionScore;
        private final Integer overallScore;
        private final String comment;
        private final Instant submittedAt;

        public InsertEvaluationCommand(
                Long studentId,
                Long offeringId,
                Integer teachingScore,
                Integer contentScore,
                Integer interactionScore,
                Integer overallScore,
                String comment,
                Instant submittedAt
        ) {
            this.studentId = studentId;
            this.offeringId = offeringId;
            this.teachingScore = teachingScore;
            this.contentScore = contentScore;
            this.interactionScore = interactionScore;
            this.overallScore = overallScore;
            this.comment = comment;
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

        public Long getOfferingId() {
            return offeringId;
        }

        public Integer getTeachingScore() {
            return teachingScore;
        }

        public Integer getContentScore() {
            return contentScore;
        }

        public Integer getInteractionScore() {
            return interactionScore;
        }

        public Integer getOverallScore() {
            return overallScore;
        }

        public String getComment() {
            return comment;
        }

        public Instant getSubmittedAt() {
            return submittedAt;
        }
    }
}
