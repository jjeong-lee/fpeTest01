package kr.ac.knue.achievement.menus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import kr.ac.knue.achievement.auth.AuthenticationPort;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MenuPermissionManagementApiTest.MenuPermissionManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MenuPermissionManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID BLOCKED_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String MENU_ID = "00000000-0000-0000-0000-000000000305";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheSelectedSubjectsMenuPermissionMatrixWithMenuHierarchy() throws Exception {
        requireOpenApiFixture();

        mockMvc.perform(get("/api/menu-permissions").cookie(adminCookie())
                        .queryParam("subjectType", "ROLE").queryParam("subjectId", "R09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].subjectType").value("ROLE"))
                .andExpect(jsonPath("$.data.content[0].subjectId").value("R09"))
                .andExpect(jsonPath("$.data.content[0].topMenuName").value("시스템 관리"))
                .andExpect(jsonPath("$.data.content[0].middleMenuName").value("역할·권한 관리"))
                .andExpect(jsonPath("$.data.content[0].menuName").value("메뉴 권한 관리"))
                .andExpect(jsonPath("$.data.content[0].accessDecision").value("ALLOW"));
    }

    @Test
    void savesAnExplicitUserDenyThenHidesTheMenuDecisionFromDirectProtectedApiAccess() throws Exception {
        mockMvc.perform(put("/api/menu-permissions").cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectType\":\"USER\",\"subjectId\":\"" + BLOCKED_ID
                                + "\",\"menuId\":\"" + MENU_ID + "\",\"accessDecision\":\"DENY\",\"reason\":\"권한 회수\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subjectId").value(BLOCKED_ID.toString()))
                .andExpect(jsonPath("$.data.accessDecision").value("DENY"));

        mockMvc.perform(get("/api/menu-permissions").cookie(adminCookie())
                        .queryParam("subjectType", "USER").queryParam("subjectId", BLOCKED_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].accessDecision").value("DENY"));

        mockMvc.perform(get("/api/menu-permissions").cookie(blockedCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MENU_ACCESS_DENIED"));
    }

    @Test
    void rejectsMissingAccessDecisionWithoutPersistingAnInvalidPermission() throws Exception {
        mockMvc.perform(put("/api/menu-permissions").cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectType\":\"USER\",\"subjectId\":\"" + BLOCKED_ID
                                + "\",\"menuId\":\"" + MENU_ID + "\",\"reason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("accessDecision"));
    }

    private void requireOpenApiFixture() {
        if (!new ClassPathResource("contracts/openapi.yaml").exists()) {
            throw new IllegalStateException("OpenAPI test fixture is required");
        }
    }

    private jakarta.servlet.http.Cookie adminCookie() { return new jakarta.servlet.http.Cookie("SESSION", "admin-session"); }
    private jakarta.servlet.http.Cookie blockedCookie() { return new jakarta.servlet.http.Cookie("SESSION", "blocked-session"); }

    @TestConfiguration
    static class MenuPermissionManagementConfiguration {
        @Bean
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final AuthenticatedSession admin = new AuthenticatedSession(ADMIN_ID, "admin-session", "admin", "KNUE", Set.of("R09"), Instant.now().plusSeconds(3600));
                private final AuthenticatedSession blocked = new AuthenticatedSession(BLOCKED_ID, "blocked-session", "blocked", "KNUE", Set.of("R09"), Instant.now().plusSeconds(3600));
                @Override public LoginResult login(String username, String password) { return LoginResult.failure(); }
                @Override public AuthenticatedSession findActiveSession(String sessionId) { return "admin-session".equals(sessionId) ? admin : "blocked-session".equals(sessionId) ? blocked : null; }
                @Override public boolean logout(String sessionId) { return false; }
            };
        }

        @Bean
        @Primary
        InMemoryMenuPermissionManagementService menuPermissionManagementService() {
            return new InMemoryMenuPermissionManagementService();
        }

        @Bean
        MenuAccessPort menuAccessPort(InMemoryMenuPermissionManagementService service) {
            return service;
        }
    }

    static class InMemoryMenuPermissionManagementService implements MenuPermissionManagementService, MenuAccessPort {
        private final List<MenuPermissionView> permissions = new ArrayList<>(List.of(
                new MenuPermissionView("ROLE", "R09", MENU_ID, "시스템 관리", "역할·권한 관리", "메뉴 권한 관리", "ALLOW")));

        @Override
        public MenuPermissionSearchResult search(String subjectType, String subjectId, int page, int size) {
            List<MenuPermissionView> content = permissions.stream().filter(permission -> (subjectType == null || subjectType.equals(permission.subjectType()))
                    && (subjectId == null || subjectId.equals(permission.subjectId()))).toList();
            return new MenuPermissionSearchResult(content, content.size(), page, size);
        }

        @Override
        public MenuPermissionView save(MenuPermissionRequest request, UUID actorUserId) {
            permissions.removeIf(permission -> permission.subjectType().equals(request.subjectType()) && permission.subjectId().equals(request.subjectId()) && permission.menuId().equals(request.menuId()));
            MenuPermissionView saved = new MenuPermissionView(request.subjectType(), request.subjectId(), request.menuId(), "시스템 관리", "역할·권한 관리", "메뉴 권한 관리", request.accessDecision());
            permissions.add(saved);
            return saved;
        }

        @Override
        public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) {
            if (!"/system/menu-permissions".equals(menuPath)) return true;
            return permissions.stream().noneMatch(permission -> permission.subjectType().equals("USER")
                    && permission.subjectId().equals(userId.toString()) && permission.accessDecision().equals("DENY"));
        }

        @Override
        public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) {
            return mayAccess(userId, organizationCode, roleCodes, "/system/menu-permissions") ? List.of("/system/menu-permissions") : List.of();
        }
    }
}
