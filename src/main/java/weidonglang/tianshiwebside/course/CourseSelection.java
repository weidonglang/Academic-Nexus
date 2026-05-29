package weidonglang.tianshiwebside.course;

import jakarta.persistence.*;
import weidonglang.tianshiwebside.student.Student;

import java.time.Instant;

/**
 * 学生选课记录实体，对应 course_selection 表。
 *
 * 一条记录表示某个学生已经选择了某个教学班。表上的唯一约束保证同一个学生不能重复选同一个教学班，
 * Redis 抢课逻辑最终也要落到这张表中，数据库唯一约束是防重复的最后一道保护。
 */
@Entity
@Table(
        name = "course_selection",
        uniqueConstraints = @UniqueConstraint(name = "uk_course_selection_student_offering", columnNames = {"student_id", "offering_id"})
)
public class CourseSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(nullable = false)
    private Instant selectedAt;

    protected CourseSelection() {
    }

    public CourseSelection(Student student, CourseOffering offering) {
        this.student = student;
        this.offering = offering;
        this.selectedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CourseOffering getOffering() {
        return offering;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }
}
