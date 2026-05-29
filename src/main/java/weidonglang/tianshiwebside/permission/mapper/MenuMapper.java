package weidonglang.tianshiwebside.permission.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper {
    @Select("""
            select
              code as code,
              title as title,
              path as path,
              icon as icon,
              parent_code as parentCode
            from sys_menu
            order by sort_order asc
            """)
    List<MenuRow> findAllOrdered();

    @Select("""
            <script>
            select distinct
              m.code as code,
              m.title as title,
              m.path as path,
              m.icon as icon,
              m.parent_code as parentCode
            from sys_menu m
            where m.code in (
              select child.code
              from sys_menu child
              join sys_role_menu rm on rm.menu_id = child.id
              join sys_role r on r.id = rm.role_id
              where r.code in
              <foreach collection="roleCodes" item="roleCode" open="(" separator="," close=")">
                #{roleCode}
              </foreach>
            )
            or m.code in (
              select child.parent_code
              from sys_menu child
              join sys_role_menu rm on rm.menu_id = child.id
              join sys_role r on r.id = rm.role_id
              where child.parent_code is not null
                and r.code in
                <foreach collection="roleCodes" item="roleCode" open="(" separator="," close=")">
                  #{roleCode}
                </foreach>
            )
            order by m.sort_order asc
            </script>
            """)
    List<MenuRow> findByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    record MenuRow(
            String code,
            String title,
            String path,
            String icon,
            String parentCode
    ) {
    }
}
