package weidonglang.tianshiwebside.academic.mapper;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AcademicAdminMapper {
    @Select("""
            select
              ag.id as grade_id,
              s.student_no as student_no,
              u.display_name as student_name,
              c.id as course_id,
              c.code as course_code,
              c.name as course_name,
              ag.term as term,
              ag.score as score,
              ag.grade_point as grade_point,
              ag.exam_type as exam_type,
              ag.grade_status as grade_status,
              ag.locked as locked
            from academic_grade ag
            join student s on s.id = ag.student_id
            join sys_user u on u.id = s.user_id
            join course c on c.id = ag.course_id
            where (#{term} is null or ag.term = #{term})
              and (#{keyword} is null or s.student_no like #{keyword} or u.display_name like #{keyword} or c.name like #{keyword})
            order by ag.term desc, c.code asc, s.student_no asc
            """)
    List<GradeAdminRow> findGrades(@Param("term") String term, @Param("keyword") String keyword);

    @Select("""
            select id
            from student
            where student_no = #{studentNo}
            """)
    Long findStudentIdByStudentNo(@Param("studentNo") String studentNo);

    @Select("""
            select count(*)
            from course
            where id = #{courseId}
            """)
    int countCourseById(@Param("courseId") Long courseId);

    @Select("""
            select count(*)
            from academic_grade
            where id = #{gradeId}
              and locked = true
            """)
    int countLockedGrade(@Param("gradeId") Long gradeId);

    @Insert("""
            insert into academic_grade (student_id, course_id, term, score, grade_point, exam_type, grade_status, locked)
            values (#{studentId}, #{courseId}, #{term}, #{score}, #{gradePoint}, #{examType}, #{gradeStatus}, #{locked})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGrade(GradeCommand command);

    @Update("""
            update academic_grade
            set score = #{score},
                grade_point = #{gradePoint},
                exam_type = #{examType},
                grade_status = #{gradeStatus},
                locked = #{locked}
            where id = #{id}
            """)
    int updateGrade(GradeCommand command);

    @Select("""
            select
              es.id as exam_id,
              co.id as offering_id,
              c.code as course_code,
              c.name as course_name,
              co.teacher_name as teacher_name,
              co.term as term,
              es.exam_time as exam_time,
              es.room as room,
              es.seat_no as seat_no,
              es.exam_type as exam_type,
              es.status as status,
              es.invigilator as invigilator
            from exam_schedule es
            join course_offering co on co.id = es.course_offering_id
            join course c on c.id = co.course_id
            where (#{term} is null or co.term = #{term})
            order by es.exam_time asc
            """)
    List<ExamAdminRow> findExams(@Param("term") String term);

    @Select("""
            select count(*)
            from course_offering
            where id = #{offeringId}
            """)
    int countOfferingById(@Param("offeringId") Long offeringId);

    @Select("""
            select u.id
            from course_selection cs
            join student s on s.id = cs.student_id
            join sys_user u on u.id = s.user_id
            where cs.offering_id = #{offeringId}
            """)
    List<Long> findSelectedUserIdsByOfferingId(@Param("offeringId") Long offeringId);

    @Insert("""
            insert into exam_schedule (course_offering_id, exam_time, room, seat_no, exam_type, status, invigilator)
            values (#{offeringId}, #{examTime}, #{room}, #{seatNo}, #{examType}, #{status}, #{invigilator})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExam(ExamCommand command);

    @Update("""
            update exam_schedule
            set course_offering_id = #{offeringId},
                exam_time = #{examTime},
                room = #{room},
                seat_no = #{seatNo},
                exam_type = #{examType},
                status = #{status},
                invigilator = #{invigilator}
            where id = #{id}
            """)
    int updateExam(ExamCommand command);

    @Delete("delete from exam_schedule where id = #{examId}")
    int deleteExam(@Param("examId") Long examId);

    record GradeAdminRow(Long gradeId, String studentNo, String studentName, Long courseId, String courseCode,
                         String courseName, String term, Integer score, BigDecimal gradePoint, String examType,
                         String gradeStatus, Boolean locked) {
    }

    record ExamAdminRow(Long examId, Long offeringId, String courseCode, String courseName, String teacherName,
                        String term, LocalDateTime examTime, String room, String seatNo, String examType,
                        String status, String invigilator) {
    }

    class GradeCommand {
        private Long id;
        private final Long studentId;
        private final Long courseId;
        private final String term;
        private final Integer score;
        private final BigDecimal gradePoint;
        private final String examType;
        private final String gradeStatus;
        private final Boolean locked;

        public GradeCommand(Long id, Long studentId, Long courseId, String term, Integer score, BigDecimal gradePoint, String examType, String gradeStatus, Boolean locked) {
            this.id = id;
            this.studentId = studentId;
            this.courseId = courseId;
            this.term = term;
            this.score = score;
            this.gradePoint = gradePoint;
            this.examType = examType;
            this.gradeStatus = gradeStatus;
            this.locked = locked;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getStudentId() { return studentId; }
        public Long getCourseId() { return courseId; }
        public String getTerm() { return term; }
        public Integer getScore() { return score; }
        public BigDecimal getGradePoint() { return gradePoint; }
        public String getExamType() { return examType; }
        public String getGradeStatus() { return gradeStatus; }
        public Boolean getLocked() { return locked; }
    }

    class ExamCommand {
        private Long id;
        private final Long offeringId;
        private final LocalDateTime examTime;
        private final String room;
        private final String seatNo;
        private final String examType;
        private final String status;
        private final String invigilator;

        public ExamCommand(Long id, Long offeringId, LocalDateTime examTime, String room, String seatNo, String examType, String status, String invigilator) {
            this.id = id;
            this.offeringId = offeringId;
            this.examTime = examTime;
            this.room = room;
            this.seatNo = seatNo;
            this.examType = examType;
            this.status = status;
            this.invigilator = invigilator;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getOfferingId() { return offeringId; }
        public LocalDateTime getExamTime() { return examTime; }
        public String getRoom() { return room; }
        public String getSeatNo() { return seatNo; }
        public String getExamType() { return examType; }
        public String getStatus() { return status; }
        public String getInvigilator() { return invigilator; }
    }
}
