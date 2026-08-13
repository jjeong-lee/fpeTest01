package kr.ac.knue.achievement.users;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserManagementMapper {
    List<UserRow> search(@Param("criteria") UserSearchCriteria criteria);
    long count(@Param("criteria") UserSearchCriteria criteria);
    UserRow findById(@Param("userId") UUID userId);
    int updateSystemEnabled(@Param("userId") UUID userId, @Param("systemEnabled") boolean systemEnabled);
    int revokeActiveRoles(@Param("userId") UUID userId);
    int countActiveRoles(@Param("roleCodes") List<String> roleCodes);
    void insertRole(@Param("userRoleId") UUID userRoleId, @Param("userId") UUID userId, @Param("roleCode") String roleCode,
            @Param("actorUserId") UUID actorUserId, @Param("reason") String reason);

    record UserRow(UUID userId, String employeeNo, String name, String organizationCode, String position,
            String employmentStatus, String duty, java.time.LocalDate retirementDate, java.time.LocalDateTime lastSyncedAt,
            boolean systemEnabled, String roleCodes) { }
}
