package kr.ac.knue.achievement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.auth.AuthenticationPort.AuthenticatedSession;
import kr.ac.knue.achievement.auth.AuthenticationPort.LoginResult;
import kr.ac.knue.achievement.menus.MenuAccessPort;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommonFoundationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadsTheStagedOpenApiContractFromTheClasspath() {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");

        org.junit.jupiter.api.Assertions.assertTrue(openApi.exists());
    }

    @Test
    void returnsTheStandardSuccessEnvelopeForHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.meta").isMap());
    }

    @Test
    void createsAnHttpOnlySameSiteSessionForValidAdminCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(header -> org.junit.jupiter.api.Assertions.assertTrue(
                        header.getResponse().getHeader("Set-Cookie").contains("HttpOnly")))
                .andExpect(header -> org.junit.jupiter.api.Assertions.assertTrue(
                        header.getResponse().getHeader("Set-Cookie").contains("SameSite=Lax")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void rejectsMissingLoginFieldsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));
    }

    @Test
    void rejectsInvalidCredentialsWithoutIssuingASessionCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("SESSION"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void returnsCurrentUserForAnActiveSessionAndInvalidatesItOnLogout() throws Exception {
        String sessionCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andReturn().getResponse().getCookie("SESSION").getValue();

        mockMvc.perform(get("/api/auth/me").cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("R09"));

        mockMvc.perform(post("/api/auth/logout").cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/auth/me").cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @TestConfiguration
    static class MockAuthenticationConfiguration {
        @Bean
        @Primary
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final Map<String, AuthenticatedSession> sessions = new ConcurrentHashMap<>();

                @Override
                public LoginResult login(String username, String password) {
                    if (!"admin".equals(username) || !"admin".equals(password)) {
                        return LoginResult.failure();
                    }
                    String sessionId = java.util.UUID.randomUUID().toString();
                    AuthenticatedSession session = new AuthenticatedSession(
                            UUID.fromString("00000000-0000-0000-0000-000000000009"), sessionId, "admin", "KNUE",
                            Set.of("R09"), Instant.now().plusSeconds(3600));
                    sessions.put(sessionId, session);
                    return LoginResult.success(session);
                }

                @Override
                public AuthenticatedSession findActiveSession(String sessionId) {
                    return sessions.get(sessionId);
                }

                @Override
                public boolean logout(String sessionId) {
                    return sessions.remove(sessionId) != null;
                }
            };
        }

        @Bean
        @Primary
        MenuAccessPort menuAccessPort() {
            return new MenuAccessPort() {
                @Override
                public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) {
                    return roleCodes.contains("R09");
                }

                @Override
                public java.util.List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) {
                    return java.util.List.of("/system/users", "/system/organizations", "/system/roles", "/system/user-roles",
                            "/system/menu-permissions", "/system/menu-structure", "/system/menu-information",
                            "/system/code-groups", "/system/detail-codes");
                }
            };
        }
    }
}
