package weidonglang.tianshiwebside.academic;

import jakarta.persistence.*;
import weidonglang.tianshiwebside.course.CourseOffering;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_schedule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_offering_id", "exam_time"})
)
public class ExamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(nullable = false)
    private LocalDateTime examTime;

    @Column(nullable = false, length = 80)
    private String room;

    @Column(nullable = false, length = 20)
    private String seatNo;

    @Column(nullable = false, length = 40)
    private String examType;

    @Column(nullable = false, length = 40)
    private String status;

    protected ExamSchedule() {
    }

    public ExamSchedule(CourseOffering courseOffering, LocalDateTime examTime, String room, String seatNo, String examType, String status) {
        this.courseOffering = courseOffering;
        this.examTime = examTime;
        this.room = room;
        this.seatNo = seatNo;
        this.examType = examType;
        this.status = status;
    }
}
