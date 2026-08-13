package kr.ac.knue.achievement.users;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisUserManagementService implements UserManagementService {
    private final UserManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisUserManagementService(UserManagementMapper mapper, AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public UserSearchResult search(UserSearchCriteria criteria) {
        return new UserSearchResult(mapper.search(criteria).stream().map(this::view).toList(), mapper.count(criteria),
                criteria.page(), criteria.size());
    }

    @Override
    public UserView updateSystemSettings(UUID userId, UserSystemSettingsRequest request, UUID actorUserId) {
        UserManagementMapper.UserRow before = mapper.findById(userId);
        if (before == null) throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        List<String> roleCodes = request.roleCodes() == null ? roleList(before.roleCodes()) : request.roleCodes();
        if (!roleCodes.isEmpty() && mapper.countActiveRoles(roleCodes) != roleCodes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE_CODE", "유효하지 않은 업무 역할이 포함되어 있습니다.");
        }
        String beforeJson = "{\"systemEnabled\":" + before.systemEnabled() + ",\"roleCodes\":\"" + before.roleCodes() + "\"}";
        String afterJson = "{\"systemEnabled\":" + request.systemEnabled() + ",\"roleCodes\":\"" + String.join(",", roleCodes) + "\"}";
        auditedCommandExecutor.execute(actorUserId, "user_account", userId.toString(), beforeJson, afterJson, request.reason(), () -> {
            mapper.updateSystemEnabled(userId, request.systemEnabled());
            if (request.roleCodes() != null) {
                mapper.revokeActiveRoles(userId);
                roleCodes.forEach(roleCode -> mapper.insertRole(UUID.randomUUID(), userId, roleCode, actorUserId, request.reason()));
            }
        });
        return view(mapper.findById(userId));
    }

    private UserView view(UserManagementMapper.UserRow row) {
        return new UserView(row.userId(), row.employeeNo(), row.name(), row.organizationCode(), row.position(),
                row.employmentStatus(), row.duty(), row.retirementDate(), row.lastSyncedAt(), row.systemEnabled(), roleList(row.roleCodes()));
    }

    private List<String> roleList(String roleCodes) {
        return roleCodes == null || roleCodes.isBlank() ? List.of() : Arrays.asList(roleCodes.split(","));
    }
}
