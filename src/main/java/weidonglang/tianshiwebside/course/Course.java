package weidonglang.tianshiwebside.course;

import jakarta.persistence.*;

@Entity
@Table(name = "course")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer credit;

    @Column(nullable = false, length = 40)
    private String category;

    protected Course() {
    }

    public Course(String code, String name, Integer credit, String category) {
        this.code = code;
        this.name = name;
        this.credit = credit;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getCredit() {
        return credit;
    }

    public String getCategory() {
        return category;
    }
}
