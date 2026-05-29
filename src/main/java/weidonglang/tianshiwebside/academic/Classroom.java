package weidonglang.tianshiwebside.academic;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom", uniqueConstraints = @UniqueConstraint(columnNames = "room"))
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String campus;

    @Column(nullable = false, length = 60)
    private String building;

    @Column(nullable = false, length = 40)
    private String room;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, length = 40)
    private String roomType;

    @Column(nullable = false, length = 40)
    private String availableSlot;

    protected Classroom() {
    }

    public Classroom(String campus, String building, String room, Integer capacity, String roomType, String availableSlot) {
        this.campus = campus;
        this.building = building;
        this.room = room;
        this.capacity = capacity;
        this.roomType = roomType;
        this.availableSlot = availableSlot;
    }
}
