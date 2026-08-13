package kr.ac.knue.achievement.roles;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisRoleManagementService implements RoleManagementService {
    private final RoleManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisRoleManagementService(RoleManagementMapper mapper, AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public RoleSearchResult search(int page, int size) {
        return new RoleSearchResult(mapper.search(size, page * size).stream().map(this::view).toList(),
                mapper.count(), page, size);
    }

    @Override
    public RoleView update(String roleCode, RoleUpdateRequest request, UUID actorUserId) {
        RoleManagementMapper.RoleRow current = requireRole(roleCode);
        validateUseStatus(request.useStatus());
        String afterRoleName = valueOrCurrent(request.roleName(), current.roleName());
        String afterGrantCriteria = valueOrCurrent(request.grantCriteria(), current.grantCriteria());
        String afterDataScopeDefault = valueOrCurrent(request.dataScopeDefault(), current.dataScopeDefault());
        String afterUseStatus = valueOrCurrent(request.useStatus(), current.useStatus());
        String beforeValue = roleJson(current);
        String afterValue = roleJson(new RoleManagementMapper.RoleRow(roleCode, afterRoleName, current.purpose(),
                afterGrantCriteria, afterDataScopeDefault, afterUseStatus));
        auditedCommandExecutor.execute(actorUserId, "role", roleCode, beforeValue, afterValue, request.reason(),
                () -> mapper.update(roleCode, afterRoleName, afterGrantCriteria, afterDataScopeDefault, afterUseStatus));
        return view(requireRole(roleCode));
    }

    private RoleManagementMapper.RoleRow requireRole(String roleCode) {
        RoleManagementMapper.RoleRow role = mapper.findByCode(roleCode);
        if (role == null) throw new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "역할을 찾을 수 없습니다.");
        return role;
    }

    private void validateUseStatus(String useStatus) {
        if (useStatus != null && !"ACTIVE".equals(useStatus) && !"INACTIVE".equals(useStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USE_STATUS", "사용 상태를 확인해 주세요.");
        }
    }

    private String valueOrCurrent(String requested, String current) { return requested == null ? current : requested; }

    private RoleView view(RoleManagementMapper.RoleRow row) {
        return new RoleView(row.roleCode(), row.roleName(), row.purpose(), row.grantCriteria(),
                row.dataScopeDefault(), row.useStatus());
    }

    private String roleJson(RoleManagementMapper.RoleRow row) {
        return "{\"roleCode\":\"" + json(row.roleCode()) + "\",\"roleName\":\"" + json(row.roleName())
                + "\",\"purpose\":\"" + json(row.purpose()) + "\",\"grantCriteria\":" + nullableJson(row.grantCriteria())
                + ",\"dataScopeDefault\":" + nullableJson(row.dataScopeDefault()) + ",\"useStatus\":\"" + json(row.useStatus()) + "\"}";
    }

    private String nullableJson(String value) { return value == null ? "null" : "\"" + json(value) + "\""; }
    private String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}