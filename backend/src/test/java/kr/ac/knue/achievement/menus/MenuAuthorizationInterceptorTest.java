package kr.ac.knue.achievement.menus;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MenuAuthorizationInterceptorTest {
    private final MenuPermissionDecisionService decisionService = new MenuPermissionDecisionService();

    @Test
    void usesTheSameExplicitDenialRuleForDirectApiAccessAsForMenuVisibility() {
        boolean menuVisible = decisionService.isAllowed("user-1", "ORG-1", Set.of("R09"), List.of(
                new MenuPermissionDecisionService.Permission("ROLE", "R09", MenuPermissionDecisionService.Decision.ALLOW),
                new MenuPermissionDecisionService.Permission("USER", "user-1", MenuPermissionDecisionService.Decision.DENY)));

        MenuAccessPort accessPort = new MenuAccessPort() {
            @Override
            public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) {
                return menuVisible;
            }

            @Override
            public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) {
                return menuVisible ? List.of("/system/users") : List.of();
            }
        };

        assertFalse(menuVisible);
        assertFalse(accessPort.mayAccess(UUID.randomUUID(), "ORG-1", Set.of("R09"), "/system/users"));
    }
}
