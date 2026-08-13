package kr.ac.knue.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.auth.AuthenticationPort.AuthenticatedSession;
import kr.ac.knue.achievement.auth.AuthenticationPort.LoginResult;
import kr.ac.knue.achievement.codes.CodeGroupManagementService;
import kr.ac.knue.achievement.codes.CodeGroupView;
import kr.ac.knue.achievement.codes.DetailCodeManagementService;
import kr.ac.knue.achievement.codes.DetailCodeView;
import kr.ac.knue.achievement.menus.MenuAccessPort;
import kr.ac.knue.achievement.menus.MenuInformationManagementService;
import kr.ac.knue.achievement.menus.MenuInformationView;
import kr.ac.knue.achievement.menus.MenuPermissionManagementService;
import kr.ac.knue.achievement.menus.MenuPermissionView;
import kr.ac.knue.achievement.menus.MenuStructureManagementService;
import kr.ac.knue.achievement.menus.MenuStructureView;
import kr.ac.knue.achievement.organizations.OrganizationManagementService;
import kr.ac.knue.achievement.organizations.OrganizationTreeView;
import kr.ac.knue.achievement.organizations.OrganizationView;
import kr.ac.knue.achievement.roles.RoleManagementService;
import kr.ac.knue.achievement.roles.RoleView;
import kr.ac.knue.achievement.userroles.UserRoleManagementService;
import kr.ac.knue.achievement.userroles.UserRoleView;
import kr.ac.knue.achievement.users.UserManagementService;
import kr.ac.knue.achievement.users.UserSearchCriteria;
import kr.ac.knue.achievement.users.UserSearchResult;
import kr.ac.knue.achievement.users.UserView;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SystemManagementSmokeApiTest.SystemManagementSmokeConfiguration.class)
class SystemManagementSmokeApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MENU_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanLogInAndReadEachOfTheNineSystemManagementApiGroups() throws Exception {
        requireOpenApiFixture();
        String sessionId = loginAsAdmin();

        for (String path : List.of(
                "/api/users",
                "/api/organizations",
                "/api/roles",
                "/api/users/10000000-0000-0000-0000-000000000001/roles",
                "/api/menu-permissions",
                "/api/menu-structure",
                "/api/menus",
                "/api/code-groups",
                "/api/code-groups/ACADEMIC_STATUS/detail-codes")) {
            mockMvc.perform(get(path).cookie(new jakarta.servlet.http.Cookie("SESSION", sessionId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Test
    void unauthenticatedUsersCannotCallProtectedSystemManagementApis() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void explicitlyDeniedUserCannotCallProtectedSystemManagementApis() throws Exception {
        mockMvc.perform(get("/api/users").cookie(new jakarta.servlet.http.Cookie("SESSION", "denied-session")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MENU_ACCESS_DENIED"));
    }

    @Test
    void doesNotExposeOutOfScopeOperationalOrSensitiveApiRoutes() throws Exception {
        String sessionId = loginAsAdmin();

        for (String path : List.of("/api/files", "/api/excel", "/api/personal-information", "/api/access-logs", "/api/audit-logs", "/api/batch")) {
            mockMvc.perform(get(path).cookie(new jakarta.servlet.http.Cookie("SESSION", sessionId)))
                    .andExpect(status().isNotFound());
        }
    }

    private String loginAsAdmin() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andReturn().getResponse().getCookie("SESSION").getValue();
    }

    private void requireOpenApiFixture() {
        org.junit.jupiter.api.Assertions.assertTrue(new ClassPathResource("contracts/openapi.yaml").exists());
    }

    @TestConfiguration
    static class SystemManagementSmokeConfiguration {
        @Bean
        @Primary
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final ConcurrentHashMap<String, AuthenticatedSession> sessions = new ConcurrentHashMap<>();

                @Override
                public LoginResult login(String username, String password) {
                    if (!"admin".equals(username) || !"admin".equals(password)) return LoginResult.failure();
                    String sessionId = UUID.randomUUID().toString();
                    AuthenticatedSession session = new AuthenticatedSession(ADMIN_ID, sessionId, "admin", "KNUE", Set.of("R09"), Instant.now().plusSeconds(3600));
                    sessions.put(sessionId, session);
                    return LoginResult.success(session);
                }

                @Override
                public AuthenticatedSession findActiveSession(String sessionId) {
                    if ("denied-session".equals(sessionId)) {
                        return new AuthenticatedSession(USER_ID, sessionId, "denied-user", "KNUE", Set.of("R01"), Instant.now().plusSeconds(3600));
                    }
                    return sessions.get(sessionId);
                }
                @Override public boolean logout(String sessionId) { return sessions.remove(sessionId) != null; }
            };
        }

        @Bean
        @Primary
        MenuAccessPort menuAccessPort() {
            List<String> paths = List.of("/system/users", "/system/organizations", "/system/roles", "/system/user-roles",
                    "/system/menu-permissions", "/system/menu-structure", "/system/menu-information", "/system/code-groups", "/system/detail-codes");
            return new MenuAccessPort() {
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && paths.contains(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return roleCodes.contains("R09") ? paths : List.of(); }
            };
        }

        @Bean
        @Primary
        UserManagementService userManagementService() {
            UserView user = new UserView(USER_ID, "20240001", "홍길동", "CS", "교수", "ACTIVE", "학과장", null, java.time.LocalDateTime.parse("2026-08-13T09:00:00"), true, List.of("R01"));
            return new UserManagementService() {
                @Override public UserSearchResult search(UserSearchCriteria criteria) { return new UserSearchResult(List.of(user), 1, criteria.page(), criteria.size()); }
                @Override public UserView updateSystemSettings(UUID userId, kr.ac.knue.achievement.users.UserSystemSettingsRequest request, UUID actorUserId) { return user; }
            };
        }

        @Bean
        @Primary
        OrganizationManagementService organizationManagementService() {
            OrganizationView organization = new OrganizationView("CS", "컴퓨터교육과", "DEPARTMENT", "ACTIVE", null);
            return new OrganizationManagementService() {
                @Override public OrganizationSearchResult search(String organizationCode, int page, int size) { return new OrganizationSearchResult(List.of(organization), 1, page, size); }
                @Override public OrganizationTreeView tree(String organizationCode) { return new OrganizationTreeView("CS", "컴퓨터교육과", "DEPARTMENT", "ACTIVE", null, List.of(), List.of()); }
                @Override public OrganizationTreeView saveRelation(String organizationCode, kr.ac.knue.achievement.organizations.OrganizationRelationRequest request, UUID actorUserId) { return tree(organizationCode); }
            };
        }

        @Bean
        @Primary
        RoleManagementService roleManagementService() {
            RoleView role = new RoleView("R09", "시스템관리자", "시스템 관리", "관리자 지정", "전체", "ACTIVE");
            return new RoleManagementService() {
                @Override public RoleSearchResult search(int page, int size) { return new RoleSearchResult(List.of(role), 1, page, size); }
                @Override public RoleView update(String roleCode, kr.ac.knue.achievement.roles.RoleUpdateRequest request, UUID actorUserId) { return role; }
            };
        }

        @Bean
        @Primary
        UserRoleManagementService userRoleManagementService() {
            UserRoleView userRole = new UserRoleView("R01", "MANUAL", null, null, ADMIN_ID, "시드 역할", "ACTIVE");
            return new UserRoleManagementService() {
                @Override public List<UserRoleView> listCurrentRoles(UUID userId) { return List.of(userRole); }
                @Override public UserRoleView grantOrUpdate(UUID userId, String roleCode, kr.ac.knue.achievement.userroles.UserRoleRequest request, UUID actorUserId) { return userRole; }
                @Override public UserRoleView revoke(UUID userId, String roleCode, kr.ac.knue.achievement.userroles.UserRoleRevocationRequest request, UUID actorUserId) { return userRole; }
            };
        }

        @Bean
        @Primary
        MenuPermissionManagementService menuPermissionManagementService() {
            MenuPermissionView permission = new MenuPermissionView("ROLE", "R09", MENU_ID.toString(), "시스템 관리", "사용자·조직 관리", "사용자 관리", "ALLOW");
            return new MenuPermissionManagementService() {
                @Override public MenuPermissionSearchResult search(String subjectType, String subjectId, int page, int size) { return new MenuPermissionSearchResult(List.of(permission), 1, page, size); }
                @Override public MenuPermissionView save(kr.ac.knue.achievement.menus.MenuPermissionRequest request, UUID actorUserId) { return permission; }
            };
        }

        @Bean
        @Primary
        MenuStructureManagementService menuStructureManagementService() {
            MenuStructureView menu = new MenuStructureView(MENU_ID, null, "사용자 관리", 1, "ACTIVE", List.of());
            return new MenuStructureManagementService() {
                @Override public List<MenuStructureView> structure() { return List.of(menu); }
                @Override public MenuStructureView updateParent(UUID menuId, kr.ac.knue.achievement.menus.MenuParentRequest request, UUID actorUserId) { return menu; }
                @Override public MenuStructureView updateDisplayOrder(UUID menuId, kr.ac.knue.achievement.menus.MenuDisplayOrderRequest request, UUID actorUserId) { return menu; }
            };
        }

        @Bean
        @Primary
        MenuInformationManagementService menuInformationManagementService() {
            MenuInformationView menu = new MenuInformationView(MENU_ID, "사용자 관리", "SCR-USER-MANAGEMENT", "/system/users", "users", "SYSTEM", "사용자 관리", "ACTIVE");
            return new MenuInformationManagementService() {
                @Override public List<MenuInformationView> listMenus() { return List.of(menu); }
                @Override public MenuInformationView updateMenu(UUID menuId, kr.ac.knue.achievement.menus.MenuInformationUpdateRequest request, UUID actorUserId) { return menu; }
            };
        }

        @Bean
        @Primary
        CodeGroupManagementService codeGroupManagementService() {
            CodeGroupView group = new CodeGroupView("ACADEMIC_STATUS", "학적 상태", "학적 상태 코드", "교무과", "ACTIVE");
            return new CodeGroupManagementService() {
                @Override public List<CodeGroupView> listCodeGroups() { return List.of(group); }
                @Override public CodeGroupView create(kr.ac.knue.achievement.codes.CodeGroupRequest request, UUID actorUserId) { return group; }
                @Override public CodeGroupView update(String groupId, kr.ac.knue.achievement.codes.CodeGroupRequest request, UUID actorUserId) { return group; }
            };
        }

        @Bean
        @Primary
        DetailCodeManagementService detailCodeManagementService() {
            DetailCodeView code = new DetailCodeView(UUID.fromString("00000000-0000-0000-0000-000000000901"), "ACADEMIC_STATUS", "ENROLLED", "재학", null, 1, java.util.Map.of(), "ACTIVE");
            return new DetailCodeManagementService() {
                @Override public List<DetailCodeView> listByGroup(String groupId) { return List.of(code); }
                @Override public DetailCodeView create(kr.ac.knue.achievement.codes.DetailCodeRequest request, UUID actorUserId) { return code; }
                @Override public DetailCodeView update(UUID detailCodeId, kr.ac.knue.achievement.codes.DetailCodeRequest request, UUID actorUserId) { return code; }
            };
        }
    }
}
