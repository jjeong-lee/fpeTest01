package kr.ac.knue.achievement.userroles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import kr.ac.knue.achievement.common.ApiException;

public class InMemoryUserRoleManagementService implements UserRoleManagementService {
    private final UUID userId;
    private final List<UserRoleView> assignments = new ArrayList<>();
    private final List<String> historyReasons = new ArrayList<>();

    public InMemoryUserRoleManagementService(UUID userId, UUID approverUserId) {
        this.userId = userId;
        assignments.add(new UserRoleView("R01", "MANUAL", LocalDate.parse("2026-03-01"), LocalDate.parse("2026-12-31"), approverUserId, "초기 역할", "ACTIVE"));
    }

    @Override
    public List<UserRoleView> listCurrentRoles(UUID requestedUserId) {
        requireUser(requestedUserId);
        return assignments.stream().filter(role -> "ACTIVE".equals(role.status())).toList();
    }

    @Override
    public UserRoleView grantOrUpdate(UUID requestedUserId, String roleCode, UserRoleRequest request, UUID actorUserId) {
        requireUser(requestedUserId);
        if (request.approverUserId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "승인자는 필수입니다.");
        if (request.reason() == null || request.reason().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "처리 사유는 필수입니다.");
        UserRoleView updated = new UserRoleView(roleCode, request.assignmentType() == null ? "MANUAL" : request.assignmentType(), request.effectiveStartDate(), request.effectiveEndDate(), request.approverUserId(), request.reason(), "ACTIVE");
        assignments.removeIf(role -> role.roleCode().equals(roleCode));
        assignments.add(updated);
        historyReasons.add(request.reason());
        return updated;
    }

    @Override
    public UserRoleView revoke(UUID requestedUserId, String roleCode, UserRoleRevocationRequest request, UUID actorUserId) {
        requireUser(requestedUserId);
        if (request.reason() == null || request.reason().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "회수 사유는 필수입니다.");
        UserRoleView current = assignments.stream().filter(role -> role.roleCode().equals(roleCode) && "ACTIVE".equals(role.status())).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_ROLE_NOT_FOUND", "사용자 역할을 찾을 수 없습니다."));
        UserRoleView revoked = new UserRoleView(current.roleCode(), current.assignmentType(), current.effectiveStartDate(), current.effectiveEndDate(), current.approverUserId(), request.reason(), "REVOKED");
        assignments.set(assignments.indexOf(current), revoked);
        historyReasons.add(request.reason());
        return revoked;
    }

    public int historyCount() { return historyReasons.size(); }
    public String lastHistoryReason() { return historyReasons.get(historyReasons.size() - 1); }
    private void requireUser(UUID requestedUserId) { if (!userId.equals(requestedUserId)) throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."); }
}
