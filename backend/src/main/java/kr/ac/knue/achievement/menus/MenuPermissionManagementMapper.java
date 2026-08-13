package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuPermissionManagementMapper {
    List<MenuPermissionRow> search(@Param("subjectType") String subjectType, @Param("subjectId") String subjectId,
            @Param("limit") int limit, @Param("offset") int offset);

    long count(@Param("subjectType") String subjectType, @Param("subjectId") String subjectId);

    MenuPermissionRow findByNaturalKey(@Param("subjectType") String subjectType, @Param("subjectId") String subjectId,
            @Param("menuId") UUID menuId);

    boolean menuExists(@Param("menuId") UUID menuId);

    int insert(@Param("permissionId") UUID permissionId, @Param("subjectType") String subjectType,
            @Param("subjectId") String subjectId, @Param("menuId") UUID menuId, @Param("accessDecision") String accessDecision);

    int update(@Param("subjectType") String subjectType, @Param("subjectId") String subjectId,
            @Param("menuId") UUID menuId, @Param("accessDecision") String accessDecision);

    record MenuPermissionRow(String subjectType, String subjectId, UUID menuId, String topMenuName,
            String middleMenuName, String menuName, String accessDecision) { }
}
