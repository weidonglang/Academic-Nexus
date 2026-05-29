package weidonglang.tianshiwebside.student;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "student_status_change_application")
public class StudentStatusChangeApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChangeType type;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant reviewedAt;

    @Column(length = 500)
    private String reviewComment;

    protected StudentStatusChangeApplication() {
    }

    public StudentStatusChangeApplication(Student student, StatusChangeType type, String reason) {
        this.student = student;
        this.type = type;
        this.reason = reason;
        this.status = ApplicationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }
}
