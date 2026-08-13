package kr.ac.knue.achievement.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.auth.AuthenticationPort.AuthenticatedSession;
import kr.ac.knue.achievement.menus.MenuAccessPort;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(UserManagementApiTest.UserManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void filtersUsersByAllSupportedConditionsAndReturnsReadonlySnapshotColumns() throws Exception {
        mockMvc.perform(get("/api/users")
                        .cookie(sessionCookie())
                        .queryParam("employeeNo", "20240001")
                        .queryParam("name", "홍길동")
                        .queryParam("organizationCode", "CS")
                        .queryParam("position", "교수")
                        .queryParam("employmentStatus", "ACTIVE")
                        .queryParam("roleCode", "R01")
                        .queryParam("systemEnabled", "true")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].employeeNo").value("20240001"))
                .andExpect(jsonPath("$.data.content[0].duty").value("학과장"))
                .andExpect(jsonPath("$.data.content[0].retirementDate").value("2038-02-28"))
                .andExpect(jsonPath("$.data.content[0].lastSyncedAt").value("2026-08-13T09:00:00"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void updatesOnlyLocalSystemSettingsAndRecordsTheChange() throws Exception {
        mockMvc.perform(patch("/api/users/{userId}/system-settings", USER_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemEnabled\":false,\"roleCodes\":[\"R09\"],\"reason\":\"업무 전환\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemEnabled").value(false))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("R09"));

        mockMvc.perform(get("/api/users")
                        .cookie(sessionCookie())
                        .queryParam("employeeNo", "20240001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].organizationCode").value("CS"))
                .andExpect(jsonPath("$.data.content[0].systemEnabled").value(false))
                .andExpect(jsonPath("$.data.content[0].roleCodes[0]").value("R09"));
    }

    @Test
    void rejectsReadonlySnapshotFieldsWithoutChangingLocalSettings() throws Exception {
        mockMvc.perform(patch("/api/users/{userId}/system-settings", USER_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemEnabled\":false,\"name\":\"변경 금지\",\"reason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));

        mockMvc.perform(get("/api/users").cookie(sessionCookie()).queryParam("employeeNo", "20240001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].systemEnabled").value(true));
    }

    private jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie("SESSION", "r09-session");
    }

    @TestConfiguration
    static class UserManagementConfiguration {
        @Bean
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final AuthenticatedSession session = new AuthenticatedSession(ADMIN_ID, "r09-session", "admin", "KNUE", java.util.Set.of("R09"), java.time.Instant.now().plusSeconds(3600));

                @Override public LoginResult login(String username, String password) { return LoginResult.failure(); }
                @Override public AuthenticatedSession findActiveSession(String sessionId) { return "r09-session".equals(sessionId) ? session : null; }
                @Override public boolean logout(String sessionId) { return false; }
            };
        }

        @Bean
        MenuAccessPort menuAccessPort() {
            return new MenuAccessPort() {
                @Override public boolean mayAccess(UUID userId, String organizationCode, java.util.Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09"); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, java.util.Set<String> roleCodes) { return List.of("/system/users"); }
            };
        }

        @Bean
        @Primary
        UserManagementService userManagementService() {
            UserView user = new UserView(USER_ID, "20240001", "홍길동", "CS", "교수", "ACTIVE", "학과장", LocalDate.parse("2038-02-28"), LocalDateTime.parse("2026-08-13T09:00:00"), true, List.of("R01"));
            return new InMemoryUserManagementService(user);
        }
    }
}
