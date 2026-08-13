package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MenuAuthorizationMapper {
    @Select("""
            SELECT m.url
            FROM menu m
            WHERE m.use_status = 'ACTIVE' AND m.url IS NOT NULL
              AND NOT EXISTS (
                SELECT 1 FROM menu_permission permission
                WHERE permission.menu_id = m.menu_id AND permission.use_status = 'ACTIVE'
                  AND permission.access_decision = 'DENY'
                  AND ((permission.subject_type = 'USER' AND permission.subject_id = CAST(#{userId} AS VARCHAR))
                    OR (permission.subject_type = 'ORGANIZATION' AND permission.subject_id = #{organizationCode})
                    OR (permission.subject_type = 'ROLE' AND permission.subject_id IN
                      (SELECT role_code FROM user_role WHERE user_id = #{userId} AND status = 'ACTIVE')))
              )
              AND EXISTS (
                SELECT 1 FROM menu_permission permission
                WHERE permission.menu_id = m.menu_id AND permission.use_status = 'ACTIVE'
                  AND permission.access_decision = 'ALLOW'
                  AND ((permission.subject_type = 'USER' AND permission.subject_id = CAST(#{userId} AS VARCHAR))
                    OR (permission.subject_type = 'ORGANIZATION' AND permission.subject_id = #{organizationCode})
                    OR (permission.subject_type = 'ROLE' AND permission.subject_id IN
                      (SELECT role_code FROM user_role WHERE user_id = #{userId} AND status = 'ACTIVE')))
              )
            ORDER BY m.display_order
            """)
    List<String> findAllowedMenuPaths(@Param("userId") UUID userId, @Param("organizationCode") String organizationCode);
}
