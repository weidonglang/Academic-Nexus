package weidonglang.tianshiwebside.academic;

import jakarta.persistence.*;
import weidonglang.tianshiwebside.course.Course;
import weidonglang.tianshiwebside.student.Student;

import java.math.BigDecimal;

/**
 * 成绩实体，对应 academic_grade 表。
 *
 * 保存学生某门课程在某学期、某种考试类型下的分数和绩点。
 * 教师录入成绩和管理员成绩管理最终都会写入或更新这张表。
 */
@Entity
@Table(
        name = "academic_grade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id", "term", "exam_type"})
)
public class AcademicGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 80)
    private String term;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal gradePoint;

    @Column(nullable = false, length = 40)
    private String examType;

    protected AcademicGrade() {
    }

    public AcademicGrade(Student student, Course course, String term, Integer score, BigDecimal gradePoint, String examType) {
        this.student = student;
        this.course = course;
        this.term = term;
        this.score = score;
        this.gradePoint = gradePoint;
        this.examType = examType;
    }
}
