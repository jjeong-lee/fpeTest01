package kr.ac.knue.achievement.userroles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
@Import(UserRoleManagementApiTest.UserRoleManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserRoleManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRoleManagementService userRoleManagementService;

    @Test
    void listsCurrentUserRolesWithEffectiveDates() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        if (!openApi.exists()) throw new IllegalStateException("OpenAPI test fixture is required");

        mockMvc.perform(get("/api/users/{userId}/roles", USER_ID).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].roleCode").value("R01"))
                .andExpect(jsonPath("$.data.content[0].effectiveStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.content[0].effectiveEndDate").value("2026-12-31"));
    }

    @Test
    void grantsChangesAndRevokesRolesWhilePreservingApproverDatesReasonsAndHistory() throws Exception {
        mockMvc.perform(put("/api/users/{userId}/roles/{roleCode}", USER_ID, "R02")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentType\":\"MANUAL\",\"effectiveStartDate\":\"2026-09-01\",\"effectiveEndDate\":\"2027-02-28\",\"approverUserId\":\"00000000-0000-0000-0000-000000000009\",\"reason\":\"학과장 보직 부여\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("R02"))
                .andExpect(jsonPath("$.data.approverUserId").value(ADMIN_ID.toString()))
                .andExpect(jsonPath("$.data.effectiveStartDate").value("2026-09-01"));

        mockMvc.perform(put("/api/users/{userId}/roles/{roleCode}", USER_ID, "R02")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentType\":\"POSITION_BASED\",\"effectiveStartDate\":\"2026-10-01\",\"effectiveEndDate\":\"2027-02-28\",\"approverUserId\":\"00000000-0000-0000-0000-000000000009\",\"reason\":\"보직 정보 반영\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentType").value("POSITION_BASED"))
                .andExpect(jsonPath("$.data.effectiveStartDate").value("2026-10-01"));

        mockMvc.perform(post("/api/users/{userId}/roles/{roleCode}/revocation", USER_ID, "R02")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"보직 종료\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        mockMvc.perform(get("/api/users/{userId}/roles", USER_ID).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].roleCode").value("R01"));

        org.junit.jupiter.api.Assertions.assertEquals(3, userRoleManagementService.historyCount());
        org.junit.jupiter.api.Assertions.assertEquals("보직 종료", userRoleManagementService.lastHistoryReason());
    }

    @Test
    void rejectsMissingApproverWithoutChangingCurrentRoles() throws Exception {
        mockMvc.perform(put("/api/users/{userId}/roles/{roleCode}", USER_ID, "R02")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"승인자 누락\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("approverUserId"));

        mockMvc.perform(get("/api/users/{userId}/roles", USER_ID).cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    private jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie("SESSION", "r09-session");
    }

    @TestConfiguration
    static class UserRoleManagementConfiguration {
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
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/user-roles".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/user-roles"); }
            };
        }

        @Bean
        @Primary
        InMemoryUserRoleManagementService userRoleManagementService() {
            return new InMemoryUserRoleManagementService(USER_ID, ADMIN_ID);
        }
    }
}
