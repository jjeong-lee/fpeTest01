package kr.ac.knue.achievement.organizations;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationManagementMapper {
    List<OrganizationRow> search(@Param("organizationCode") String organizationCode, @Param("limit") int limit,
            @Param("offset") int offset);
    long count(@Param("organizationCode") String organizationCode);
    OrganizationRow findByCode(@Param("organizationCode") String organizationCode);
    OrganizationRow findEffectiveParent(@Param("organizationCode") String organizationCode, @Param("onDate") LocalDate onDate);
    List<OrganizationRow> findEffectiveChildren(@Param("parentOrganizationCode") String parentOrganizationCode,
            @Param("onDate") LocalDate onDate);
    List<RelationRow> findHistory(@Param("organizationCode") String organizationCode);
    RelationRow findActiveRelation(@Param("organizationCode") String organizationCode, @Param("onDate") LocalDate onDate);
    int closeRelation(@Param("relationId") UUID relationId, @Param("effectiveEndDate") LocalDate effectiveEndDate);
    int insertRelation(@Param("relationId") UUID relationId, @Param("organizationCode") String organizationCode,
            @Param("parentOrganizationCode") String parentOrganizationCode, @Param("effectiveStartDate") LocalDate effectiveStartDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate);

    record OrganizationRow(String organizationCode, String organizationName, String organizationType, String useStatus,
            String currentParentOrganizationCode) { }
    record RelationRow(UUID relationId, String organizationCode, String parentOrganizationCode,
            LocalDate effectiveStartDate, LocalDate effectiveEndDate, String status) { }
}
