package kr.ac.knue.achievement.userroles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleManagementMapper {
    List<UserRoleRow> findActiveByUserId(@Param("userId") UUID userId);
    UserRoleRow findByUserAndRole(@Param("userId") UUID userId, @Param("roleCode") String roleCode);
    int countUser(@Param("userId") UUID userId);
    int countRole(@Param("roleCode") String roleCode);
    int insert(@Param("userRoleId") UUID userRoleId, @Param("userId") UUID userId, @Param("roleCode") String roleCode,
            @Param("assignmentType") String assignmentType, @Param("effectiveStartDate") LocalDate effectiveStartDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate, @Param("approverUserId") UUID approverUserId,
            @Param("reason") String reason);
    int reactivate(@Param("userId") UUID userId, @Param("roleCode") String roleCode,
            @Param("assignmentType") String assignmentType, @Param("effectiveStartDate") LocalDate effectiveStartDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate, @Param("approverUserId") UUID approverUserId,
            @Param("reason") String reason);
    int revoke(@Param("userId") UUID userId, @Param("roleCode") String roleCode, @Param("reason") String reason);

    record UserRoleRow(String roleCode, String assignmentType, LocalDate effectiveStartDate,
            LocalDate effectiveEndDate, UUID approverUserId, String reason, String status) { }
}