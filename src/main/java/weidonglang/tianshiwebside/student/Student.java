package weidonglang.tianshiwebside.student;

import jakarta.persistence.*;
import weidonglang.tianshiwebside.user.SysUser;

/**
 * 学生档案实体，对应 student 表。
 *
 * 该表保存学号、学院、专业、班级、年级、学籍状态和联系方式。
 * 它通过 user_id 关联 sys_user，表示“哪个登录账号对应哪个学生档案”。
 */
@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private SysUser user;

    @Column(nullable = false, unique = true, length = 32)
    private String studentNo;

    @Column(nullable = false, length = 80)
    private String college;

    @Column(nullable = false, length = 80)
    private String major;

    @Column(nullable = false, length = 80)
    private String className;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String address;

    protected Student() {
    }

    public Student(SysUser user, String studentNo, String college, String major, String className, String grade, String status) {
        this.user = user;
        this.studentNo = studentNo;
        this.college = college;
        this.major = major;
        this.className = className;
        this.grade = grade;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public SysUser getUser() {
        return user;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getCollege() {
        return college;
    }

    public String getMajor() {
        return major;
    }

    public String getClassName() {
        return className;
    }

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public void updateContact(String phone, String email, String address) {
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
}
