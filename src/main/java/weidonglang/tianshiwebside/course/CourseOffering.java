package weidonglang.tianshiwebside.course;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 教学班实体，对应 course_offering 表。
 *
 * 课程本身只描述课程名称、编号和学分；教学班描述某门课程在某学期由谁上课、
 * 容量是多少、上课时间地点是什么。抢课压测使用的是 offeringId，而不是 courseId。
 */
@Entity
@Table(name = "course_offering")
public class CourseOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 80)
    private String teacherName;

    @Column(nullable = false, length = 80)
    private String term;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, length = 120)
    private String scheduleText;

    @Column(nullable = false, length = 120)
    private String classroom;

    @Column(nullable = false)
    private Instant selectionStartAt;

    @Column(nullable = false)
    private Instant selectionEndAt;

    protected CourseOffering() {
    }

    public CourseOffering(
            Course course,
            String teacherName,
            String term,
            Integer capacity,
            String scheduleText,
            String classroom,
            Instant selectionStartAt,
            Instant selectionEndAt
    ) {
        this.course = course;
        this.teacherName = teacherName;
        this.term = term;
        this.capacity = capacity;
        this.scheduleText = scheduleText;
        this.classroom = classroom;
        this.selectionStartAt = selectionStartAt;
        this.selectionEndAt = selectionEndAt;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getTerm() {
        return term;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getScheduleText() {
        return scheduleText;
    }

    public String getClassroom() {
        return classroom;
    }

    public Instant getSelectionStartAt() {
        return selectionStartAt;
    }

    public Instant getSelectionEndAt() {
        return selectionEndAt;
    }
}
