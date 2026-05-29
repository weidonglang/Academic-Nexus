package weidonglang.tianshiwebside.permission;

import jakarta.persistence.*;

@Entity
@Table(name = "sys_menu")
public class SysMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 120)
    private String path;

    @Column(nullable = false, length = 60)
    private String icon;

    @Column(length = 80)
    private String parentCode;

    @Column(nullable = false)
    private int sortOrder;

    protected SysMenu() {
    }

    public SysMenu(String code, String title, String path, String icon, String parentCode, int sortOrder) {
        this.code = code;
        this.title = title;
        this.path = path;
        this.icon = icon;
        this.parentCode = parentCode;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public String getIcon() {
        return icon;
    }

    public String getParentCode() {
        return parentCode;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
