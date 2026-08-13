package kr.ac.knue.achievement.roles;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleManagementMapper {
    List<RoleRow> search(@Param("limit") int limit, @Param("offset") int offset);
    long count();
    RoleRow findByCode(@Param("roleCode") String roleCode);
    int update(@Param("roleCode") String roleCode, @Param("roleName") String roleName,
            @Param("grantCriteria") String grantCriteria, @Param("dataScopeDefault") String dataScopeDefault,
            @Param("useStatus") String useStatus);

    record RoleRow(String roleCode, String roleName, String purpose, String grantCriteria,
            String dataScopeDefault, String useStatus) { }
}