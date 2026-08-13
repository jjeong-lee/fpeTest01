package kr.ac.knue.achievement.userroles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisUserRoleManagementService implements UserRoleManagementService {
    private final UserRoleManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisUserRoleManagementService(UserRoleManagementMapper mapper, AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public List<UserRoleView> listCurrentRoles(UUID userId) {
        requireUser(userId);
        return mapper.findActiveByUserId(userId).stream().map(this::view).toList();
    }

    @Override
    public UserRoleView grantOrUpdate(UUID userId, String roleCode, UserRoleRequest request, UUID actorUserId) {
        requireUser(userId);
        requireRole(roleCode);
        validate(request);
        requireUser(request.approverUserId());
        UserRoleManagementMapper.UserRoleRow before = mapper.findByUserAndRole(userId, roleCode);
        String beforeValue = before == null ? "null" : json(before);
        auditedCommandExecutor.execute(actorUserId, "user_role", userId + ":" + roleCode, beforeValue,
                json(roleCode, request, "ACTIVE"), request.reason(), () -> {
                    if (before == null) mapper.insert(UUID.randomUUID(), userId, roleCode, assignmentType(request.assignmentType()),
                            request.effectiveStartDate(), request.effectiveEndDate(), request.approverUserId(), request.reason());
                    else mapper.reactivate(userId, roleCode, assignmentType(request.assignmentType()), request.effectiveStartDate(),
                            request.effectiveEndDate(), request.approverUserId(), request.reason());
                });
        return view(requireAssignment(userId, roleCode));
    }

    @Override
    public UserRoleView revoke(UUID userId, String roleCode, UserRoleRevocationRequest request, UUID actorUserId) {
        requireUser(userId);
        if (request.reason() == null || request.reason().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "회수 사유는 필수입니다.");
        UserRoleManagementMapper.UserRoleRow before = requireAssignment(userId, roleCode);
        if (!"ACTIVE".equals(before.status())) throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_ALREADY_REVOKED", "이미 회수된 역할입니다.");
        auditedCommandExecutor.execute(actorUserId, "user_role", userId + ":" + roleCode, json(before),
                json(roleCode, new UserRoleRequest(before.assignmentType(), before.effectiveStartDate(), before.effectiveEndDate(), before.approverUserId(), request.reason()), "REVOKED"),
                request.reason(), () -> mapper.revoke(userId, roleCode, request.reason()));
        return view(requireAssignment(userId, roleCode));
    }

    private void requireUser(UUID userId) { if (mapper.countUser(userId) == 0) throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."); }
    private void requireRole(String roleCode) { if (mapper.countRole(roleCode) == 0) throw new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."); }
    private UserRoleManagementMapper.UserRoleRow requireAssignment(UUID userId, String roleCode) {
        UserRoleManagementMapper.UserRoleRow row = mapper.findByUserAndRole(userId, roleCode);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "USER_ROLE_NOT_FOUND", "사용자 역할을 찾을 수 없습니다.");
        return row;
    }
    private void validate(UserRoleRequest request) {
        if (request.approverUserId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "승인자는 필수입니다.");
        if (request.reason() == null || request.reason().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "처리 사유는 필수입니다.");
        if (request.effectiveStartDate() != null && request.effectiveEndDate() != null && request.effectiveEndDate().isBefore(request.effectiveStartDate())) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EFFECTIVE_PERIOD", "유효 종료일은 시작일보다 앞설 수 없습니다.");
        if (request.assignmentType() != null && !"MANUAL".equals(request.assignmentType()) && !"POSITION_BASED".equals(request.assignmentType())) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_TYPE", "부여 구분을 확인해 주세요.");
    }
    private String assignmentType(String value) { return value == null ? "MANUAL" : value; }
    private UserRoleView view(UserRoleManagementMapper.UserRoleRow row) { return new UserRoleView(row.roleCode(), row.assignmentType(), row.effectiveStartDate(), row.effectiveEndDate(), row.approverUserId(), row.reason(), row.status()); }
    private String json(UserRoleManagementMapper.UserRoleRow row) { return json(row.roleCode(), new UserRoleRequest(row.assignmentType(), row.effectiveStartDate(), row.effectiveEndDate(), row.approverUserId(), row.reason()), row.status()); }
    private String json(String roleCode, UserRoleRequest request, String status) {
        return "{\"roleCode\":\"" + escape(roleCode) + "\",\"assignmentType\":\"" + escape(assignmentType(request.assignmentType()))
                + "\",\"effectiveStartDate\":" + nullableDate(request.effectiveStartDate()) + ",\"effectiveEndDate\":"
                + nullableDate(request.effectiveEndDate()) + ",\"approverUserId\":\"" + request.approverUserId()
                + "\",\"reason\":\"" + escape(request.reason()) + "\",\"status\":\"" + status + "\"}";
    }
    private String nullableDate(LocalDate value) { return value == null ? "null" : "\"" + value + "\""; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}