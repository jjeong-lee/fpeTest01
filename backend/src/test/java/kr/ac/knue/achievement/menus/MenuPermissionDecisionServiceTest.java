package kr.ac.knue.achievement.menus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MenuPermissionDecisionServiceTest {
    private final MenuPermissionDecisionService service = new MenuPermissionDecisionService();

    @Test
    void allowsAccessWhenTheApplicableUserOrganizationAndRoleRulesAllAllow() {
        boolean allowed = service.isAllowed("user-1", "ORG-1", Set.of("R01"), List.of(
                new MenuPermissionDecisionService.Permission("ROLE", "R01", MenuPermissionDecisionService.Decision.ALLOW),
                new MenuPermissionDecisionService.Permission("ORGANIZATION", "ORG-1", MenuPermissionDecisionService.Decision.ALLOW),
                new MenuPermissionDecisionService.Permission("USER", "user-1", MenuPermissionDecisionService.Decision.ALLOW)));

        assertTrue(allowed);
    }

    @Test
    void deniesAccessWhenAnyApplicableScopeExplicitlyDenies() {
        boolean allowed = service.isAllowed("user-1", "ORG-1", Set.of("R01"), List.of(
                new MenuPermissionDecisionService.Permission("USER", "user-1", MenuPermissionDecisionService.Decision.ALLOW),
                new MenuPermissionDecisionService.Permission("ORGANIZATION", "ORG-1", MenuPermissionDecisionService.Decision.DENY),
                new MenuPermissionDecisionService.Permission("ROLE", "R01", MenuPermissionDecisionService.Decision.ALLOW)));

        assertFalse(allowed);
    }
}
