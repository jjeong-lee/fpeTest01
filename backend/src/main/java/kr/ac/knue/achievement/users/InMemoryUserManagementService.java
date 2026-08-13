package kr.ac.knue.achievement.users;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kr.ac.knue.achievement.common.ApiException;
import org.springframework.http.HttpStatus;

public class InMemoryUserManagementService implements UserManagementService {
    private UserView user;

    public InMemoryUserManagementService(UserView user) {
        this.user = user;
    }

    @Override
    public UserSearchResult search(UserSearchCriteria criteria) {
        if (!matches(criteria)) return new UserSearchResult(List.of(), 0, criteria.page(), criteria.size());
        return new UserSearchResult(List.of(user), 1, criteria.page(), criteria.size());
    }

    @Override
    public UserView updateSystemSettings(UUID userId, UserSystemSettingsRequest request, UUID actorUserId) {
        if (!user.userId().equals(userId)) throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        List<String> roles = request.roleCodes() == null ? user.roleCodes() : new ArrayList<>(request.roleCodes());
        user = new UserView(user.userId(), user.employeeNo(), user.name(), user.organizationCode(), user.position(),
                user.employmentStatus(), user.duty(), user.retirementDate(), user.lastSyncedAt(), request.systemEnabled(), roles);
        return user;
    }

    private boolean matches(UserSearchCriteria criteria) {
        return contains(user.employeeNo(), criteria.employeeNo()) && contains(user.name(), criteria.name())
                && equalsWhenPresent(user.organizationCode(), criteria.organizationCode())
                && equalsWhenPresent(user.position(), criteria.position())
                && equalsWhenPresent(user.employmentStatus(), criteria.employmentStatus())
                && (criteria.roleCode() == null || user.roleCodes().contains(criteria.roleCode()))
                && (criteria.systemEnabled() == null || user.systemEnabled() == criteria.systemEnabled());
    }

    private boolean contains(String value, String filter) { return filter == null || filter.isBlank() || value.contains(filter); }
    private boolean equalsWhenPresent(String value, String filter) { return filter == null || filter.isBlank() || value.equals(filter); }
}
