package kr.ac.knue.achievement.roles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import kr.ac.knue.achievement.menus.MenuAccessPort;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RoleManagementApiTest.RoleManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAllSeededRoleCodesWithTheirPurposes() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        if (!openApi.exists()) throw new IllegalStateException("OpenAPI test fixture is required");

        mockMvc.perform(get("/api/roles").cookie(sessionCookie()).queryParam("page", "0").queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(9))
                .andExpect(jsonPath("$.data.content[0].roleCode").value("R01"))
                .andExpect(jsonPath("$.data.content[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$.data.content[8].roleCode").value("R09"))
                .andExpect(jsonPath("$.data.content[8].purpose").isNotEmpty());
    }

    @Test
    void updatesRolePolicyAndNameWhileKeepingThePathRoleCodeStableOnSubsequentRead() throws Exception {
        mockMvc.perform(put("/api/roles/{roleCode}", "R04")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"교수지원 행정\",\"grantCriteria\":\"기준정보 담당자\",\"dataScopeDefault\":\"전교\",\"useStatus\":\"ACTIVE\",\"reason\":\"역할 정책 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("R04"))
                .andExpect(jsonPath("$.data.roleName").value("교수지원 행정"))
                .andExpect(jsonPath("$.data.grantCriteria").value("기준정보 담당자"))
                .andExpect(jsonPath("$.data.dataScopeDefault").value("전교"));

        mockMvc.perform(get("/api/roles").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[3].roleCode").value("R04"))
                .andExpect(jsonPath("$.data.content[3].roleName").value("교수지원 행정"));
    }

    @Test
    void rejectsRoleCodeMutationAttemptWithoutChangingTheExistingRole() throws Exception {
        mockMvc.perform(put("/api/roles/{roleCode}", "R04")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"R99\",\"roleName\":\"변경 불가\",\"reason\":\"불변 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("roleCode"));

        mockMvc.perform(get("/api/roles").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[3].roleCode").value("R04"))
                .andExpect(jsonPath("$.data.content[3].roleName").value("교수지원과"));
    }

    @Test
    void rejectsARequestWithoutAChangeReason() throws Exception {
        mockMvc.perform(put("/api/roles/{roleCode}", "R04")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"교수지원 행정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("reason"));
    }

    private jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie("SESSION", "r09-session");
    }

    @TestConfiguration
    static class RoleManagementConfiguration {
        @Bean
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final AuthenticatedSession session = new AuthenticatedSession(ADMIN_ID, "r09-session", "admin", "KNUE", Set.of("R09"), java.time.Instant.now().plusSeconds(3600));
                @Override public LoginResult login(String username, String password) { return LoginResult.failure(); }
                @Override public AuthenticatedSession findActiveSession(String sessionId) { return "r09-session".equals(sessionId) ? session : null; }
                @Override public boolean logout(String sessionId) { return false; }
            };
        }

        @Bean
        MenuAccessPort menuAccessPort() {
            return new MenuAccessPort() {
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/roles".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/roles"); }
            };
        }

        @Bean
        @Primary
        RoleManagementService roleManagementService() {
            return new InMemoryRoleManagementService();
        }
    }
}
