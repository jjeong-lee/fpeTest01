package kr.ac.knue.achievement.menus;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class MenuPermissionDecisionService {
    public enum Decision { ALLOW, DENY }

    public record Permission(String subjectType, String subjectId, Decision decision) { }

    public boolean isAllowed(String userId, String organizationCode, Set<String> roleCodes,
            Collection<Permission> permissions) {
        List<Permission> applicable = permissions.stream()
                .filter(permission -> appliesTo(permission, userId, organizationCode, roleCodes))
                .toList();

        if (applicable.stream().anyMatch(permission -> permission.decision() == Decision.DENY)) {
            return false;
        }
        return applicable.stream().anyMatch(permission -> permission.decision() == Decision.ALLOW);
    }

    private boolean appliesTo(Permission permission, String userId, String organizationCode, Set<String> roleCodes) {
        return switch (permission.subjectType()) {
            case "USER" -> permission.subjectId().equals(userId);
            case "ORGANIZATION" -> permission.subjectId().equals(organizationCode);
            case "ROLE" -> roleCodes.contains(permission.subjectId());
            default -> false;
        };
    }
}
