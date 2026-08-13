package kr.ac.knue.achievement.menus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
@Import(MenuStructureManagementApiTest.MenuStructureManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MenuStructureManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final String TOP_MENU_ID = "00000000-0000-0000-0000-000000000201";
    private static final String MENU_MANAGEMENT_ID = "00000000-0000-0000-0000-000000000213";
    private static final String STRUCTURE_MENU_ID = "00000000-0000-0000-0000-000000000306";
    private static final String INFORMATION_MENU_ID = "00000000-0000-0000-0000-000000000307";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheSeededTopMiddleAndLeafMenuTree() throws Exception {
        requireOpenApiFixture();

        mockMvc.perform(get("/api/menu-structure").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].menuId").value(TOP_MENU_ID))
                .andExpect(jsonPath("$.data[0].children[0].menuId").value(MENU_MANAGEMENT_ID))
                .andExpect(jsonPath("$.data[0].children[0].children[0].menuId").value(STRUCTURE_MENU_ID));
    }

    @Test
    void changesParentAndSiblingDisplayOrderThenReturnsTheUpdatedTreeWithoutDeletingMenus() throws Exception {
        mockMvc.perform(put("/api/menus/{menuId}/parent", INFORMATION_MENU_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentMenuId\":\"" + TOP_MENU_ID + "\",\"reason\":\"메뉴 분류 조정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentMenuId").value(TOP_MENU_ID));

        mockMvc.perform(put("/api/menus/{menuId}/display-order", STRUCTURE_MENU_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":2,\"reason\":\"표시 순서 조정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayOrder").value(2));

        mockMvc.perform(get("/api/menu-structure").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].menuId").value(INFORMATION_MENU_ID))
                .andExpect(jsonPath("$.data[0].children[1].menuId").value(MENU_MANAGEMENT_ID))
                .andExpect(jsonPath("$.data[0].children[1].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[1].children[0].menuId").value(STRUCTURE_MENU_ID));
    }

    @Test
    void rejectsSelfParentAndKeepsTheExistingMenuRelationship() throws Exception {
        mockMvc.perform(put("/api/menus/{menuId}/parent", STRUCTURE_MENU_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentMenuId\":\"" + STRUCTURE_MENU_ID + "\",\"reason\":\"잘못된 관계\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("parentMenuId"));

        mockMvc.perform(get("/api/menu-structure").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].children[0].menuId").value(STRUCTURE_MENU_ID));
    }

    private void requireOpenApiFixture() {
        if (!new ClassPathResource("contracts/openapi.yaml").exists()) throw new IllegalStateException("OpenAPI test fixture is required");
    }

    private jakarta.servlet.http.Cookie sessionCookie() { return new jakarta.servlet.http.Cookie("SESSION", "r09-session"); }

    @TestConfiguration
    static class MenuStructureManagementConfiguration {
        @Bean
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final AuthenticatedSession session = new AuthenticatedSession(ADMIN_ID, "r09-session", "admin", "KNUE", Set.of("R09"), Instant.now().plusSeconds(3600));
                @Override public LoginResult login(String username, String password) { return LoginResult.failure(); }
                @Override public AuthenticatedSession findActiveSession(String sessionId) { return "r09-session".equals(sessionId) ? session : null; }
                @Override public boolean logout(String sessionId) { return false; }
            };
        }

        @Bean
        MenuAccessPort menuAccessPort() {
            return new MenuAccessPort() {
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/menu-structure".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/menu-structure"); }
            };
        }

        @Bean
        @Primary
        MenuStructureManagementService menuStructureManagementService() { return new InMemoryMenuStructureManagementService(); }
    }

    static class InMemoryMenuStructureManagementService implements MenuStructureManagementService {
        private final List<MenuStructureView> menus = new ArrayList<>(List.of(
                new MenuStructureView(UUID.fromString(TOP_MENU_ID), null, "시스템 관리", 1, "ACTIVE", List.of()),
                new MenuStructureView(UUID.fromString(MENU_MANAGEMENT_ID), UUID.fromString(TOP_MENU_ID), "메뉴 관리", 3, "ACTIVE", List.of()),
                new MenuStructureView(UUID.fromString(STRUCTURE_MENU_ID), UUID.fromString(MENU_MANAGEMENT_ID), "메뉴 구조 관리", 1, "ACTIVE", List.of()),
                new MenuStructureView(UUID.fromString(INFORMATION_MENU_ID), UUID.fromString(MENU_MANAGEMENT_ID), "메뉴 정보 관리", 2, "ACTIVE", List.of())));

        @Override public List<MenuStructureView> structure() { return tree(null); }
        @Override public MenuStructureView updateParent(UUID menuId, MenuParentRequest request, UUID actorUserId) {
            if (menuId.equals(request.parentMenuId())) throw new kr.ac.knue.achievement.common.CommandValidationException(
                    new kr.ac.knue.achievement.common.ApiError.FieldError("parentMenuId", "자기 자신을 부모메뉴로 지정할 수 없습니다."));
            replace(menuId, menu -> new MenuStructureView(menu.menuId(), request.parentMenuId(), menu.menuName(), menu.displayOrder(), menu.useStatus(), List.of()));
            return find(menuId);
        }
        @Override public MenuStructureView updateDisplayOrder(UUID menuId, MenuDisplayOrderRequest request, UUID actorUserId) {
            replace(menuId, menu -> new MenuStructureView(menu.menuId(), menu.parentMenuId(), menu.menuName(), request.displayOrder(), menu.useStatus(), List.of()));
            return find(menuId);
        }
        private List<MenuStructureView> tree(UUID parentMenuId) {
            return menus.stream().filter(menu -> Objects.equals(parentMenuId, menu.parentMenuId())).sorted(Comparator.comparingInt(MenuStructureView::displayOrder))
                    .map(menu -> new MenuStructureView(menu.menuId(), menu.parentMenuId(), menu.menuName(), menu.displayOrder(), menu.useStatus(), tree(menu.menuId()))).toList();
        }
        private MenuStructureView find(UUID menuId) { return menus.stream().filter(menu -> menu.menuId().equals(menuId)).findFirst().orElseThrow(); }
        private void replace(UUID menuId, java.util.function.Function<MenuStructureView, MenuStructureView> change) {
            for (int index = 0; index < menus.size(); index++) if (menus.get(index).menuId().equals(menuId)) { menus.set(index, change.apply(menus.get(index))); return; }
            throw new IllegalArgumentException("menu not found");
        }
    }
}
