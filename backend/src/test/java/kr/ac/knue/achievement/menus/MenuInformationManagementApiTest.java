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
@Import(MenuInformationManagementApiTest.MenuInformationManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MenuInformationManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final String MENU_ID = "00000000-0000-0000-0000-000000000307";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsMenuExecutionInformationIncludingAllRequiredFields() throws Exception {
        requireOpenApiFixture();

        mockMvc.perform(get("/api/menus").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].menuName").value("메뉴 정보 관리"))
                .andExpect(jsonPath("$.data[0].screenId").value("SCR-MENU-INFORMATION-MANAGEMENT"))
                .andExpect(jsonPath("$.data[0].url").value("/system/menu-information"))
                .andExpect(jsonPath("$.data[0].icon").value("settings"))
                .andExpect(jsonPath("$.data[0].businessCategory").value("시스템 관리"))
                .andExpect(jsonPath("$.data[0].description").value("메뉴 실행정보와 화면 연결을 관리합니다."));
    }

    @Test
    void savesExecutionInformationAndReturnsTheConnectedScreenAndUrlOnReload() throws Exception {
        mockMvc.perform(put("/api/menus/{menuId}", MENU_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"메뉴 실행정보 관리\",\"screenId\":\"SCR-MENU-EXECUTION-MANAGEMENT\",\"url\":\"/system/menu-information/execution\",\"icon\":\"edit\",\"businessCategory\":\"시스템 관리\",\"description\":\"실행 화면 연결을 갱신합니다.\",\"reason\":\"화면 연결 갱신\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenId").value("SCR-MENU-EXECUTION-MANAGEMENT"))
                .andExpect(jsonPath("$.data.url").value("/system/menu-information/execution"));

        mockMvc.perform(get("/api/menus").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].menuName").value("메뉴 실행정보 관리"))
                .andExpect(jsonPath("$.data[0].screenId").value("SCR-MENU-EXECUTION-MANAGEMENT"))
                .andExpect(jsonPath("$.data[0].url").value("/system/menu-information/execution"));
    }

    @Test
    void rejectsMissingScreenIdAndKeepsExistingExecutionInformation() throws Exception {
        mockMvc.perform(put("/api/menus/{menuId}", MENU_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"메뉴 정보 관리\",\"url\":\"/system/menu-information\",\"reason\":\"검증 확인\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("screenId"));

        mockMvc.perform(get("/api/menus").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].screenId").value("SCR-MENU-INFORMATION-MANAGEMENT"));
    }

    private void requireOpenApiFixture() {
        if (!new ClassPathResource("contracts/openapi.yaml").exists()) throw new IllegalStateException("OpenAPI test fixture is required");
    }

    private jakarta.servlet.http.Cookie sessionCookie() { return new jakarta.servlet.http.Cookie("SESSION", "r09-session"); }

    @TestConfiguration
    static class MenuInformationManagementConfiguration {
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
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/menu-information".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/menu-information"); }
            };
        }

        @Bean
        @Primary
        MenuInformationManagementService menuInformationManagementService() { return new InMemoryMenuInformationManagementService(); }
    }

    static class InMemoryMenuInformationManagementService implements MenuInformationManagementService {
        private final List<MenuInformationView> menus = new ArrayList<>(List.of(new MenuInformationView(UUID.fromString(MENU_ID), "메뉴 정보 관리", "SCR-MENU-INFORMATION-MANAGEMENT", "/system/menu-information", "settings", "시스템 관리", "메뉴 실행정보와 화면 연결을 관리합니다.", "ACTIVE")));

        @Override public List<MenuInformationView> listMenus() { return List.copyOf(menus); }
        @Override public MenuInformationView updateMenu(UUID menuId, MenuInformationUpdateRequest request, UUID actorUserId) {
            MenuInformationView updated = new MenuInformationView(menuId, request.menuName(), request.screenId(), request.url(), request.icon(), request.businessCategory(), request.description(), "ACTIVE");
            menus.set(0, updated);
            return updated;
        }
    }
}
