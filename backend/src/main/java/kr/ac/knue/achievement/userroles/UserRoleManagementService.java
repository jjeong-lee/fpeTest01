package kr.ac.knue.achievement.userroles;

import java.util.List;
import java.util.UUID;

public interface UserRoleManagementService {
    List<UserRoleView> listCurrentRoles(UUID userId);
    UserRoleView grantOrUpdate(UUID userId, String roleCode, UserRoleRequest request, UUID actorUserId);
    UserRoleView revoke(UUID userId, String roleCode, UserRoleRevocationRequest request, UUID actorUserId);
}