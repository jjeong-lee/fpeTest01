package kr.ac.knue.achievement.organizations;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
@Import(OrganizationManagementApiTest.OrganizationManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrganizationManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsOrganizationsByCodeAndReturnsTheEffectiveParent() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        if (!openApi.exists()) throw new IllegalStateException("OpenAPI test fixture is required");

        mockMvc.perform(get("/api/organizations")
                        .cookie(sessionCookie())
                        .queryParam("organizationCode", "CS")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].organizationCode").value("CS"))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("컴퓨터교육과"))
                .andExpect(jsonPath("$.data.content[0].organizationType").value("DEPARTMENT"))
                .andExpect(jsonPath("$.data.content[0].currentParentOrganizationCode").value("COLLEGE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void returnsTheSelectedOrganizationWithItsParentAndChildrenAsATree() throws Exception {
        mockMvc.perform(get("/api/organizations/tree")
                        .cookie(sessionCookie())
                        .queryParam("organizationCode", "COLLEGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationCode").value("COLLEGE"))
                .andExpect(jsonPath("$.data.parent.organizationCode").value("KNUE"))
                .andExpect(jsonPath("$.data.children[0].organizationCode").value("CS"));
    }

    @Test
    void savesANonOverlappingRelationAndPreservesThePriorRelationInHistory() throws Exception {
        mockMvc.perform(put("/api/organization-relations/{organizationId}", "CS")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"GRAD\",\"effectiveStartDate\":\"2099-01-01\",\"reason\":\"조직 개편\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parent.organizationCode").value("COLLEGE"))
                .andExpect(jsonPath("$.data.relationHistory.length()").value(2))
                .andExpect(jsonPath("$.data.relationHistory[0].effectiveEndDate").value("2098-12-31"))
                .andExpect(jsonPath("$.data.relationHistory[1].effectiveStartDate").value("2099-01-01"));

        mockMvc.perform(get("/api/organizations/tree")
                        .cookie(sessionCookie())
                        .queryParam("organizationCode", "CS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parent.organizationCode").value("COLLEGE"))
                .andExpect(jsonPath("$.data.relationHistory.length()").value(2));
    }

    @Test
    void rejectsAnOverlappingEffectiveRelationWithoutChangingTheCurrentRelation() throws Exception {
        mockMvc.perform(put("/api/organization-relations/{organizationId}", "CS")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"GRAD\",\"effectiveStartDate\":\"2026-06-01\",\"reason\":\"충돌 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RELATION_PERIOD_CONFLICT"));

        mockMvc.perform(get("/api/organizations/tree")
                        .cookie(sessionCookie())
                        .queryParam("organizationCode", "CS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parent.organizationCode").value("COLLEGE"))
                .andExpect(jsonPath("$.data.relationHistory.length()").value(1));
    }

    @Test
    void rejectsAMissingEffectiveStartDateWithoutChangingTheRelation() throws Exception {
        mockMvc.perform(put("/api/organization-relations/{organizationId}", "CS")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"GRAD\",\"reason\":\"필수값 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("effectiveStartDate"));

        mockMvc.perform(get("/api/organizations/tree")
                        .cookie(sessionCookie())
                        .queryParam("organizationCode", "CS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parent.organizationCode").value("COLLEGE"));
    }

    private jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie("SESSION", "r09-session");
    }

    @TestConfiguration
    static class OrganizationManagementConfiguration {
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
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09"); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/organizations"); }
            };
        }

        @Bean
        @Primary
        OrganizationManagementService organizationManagementService() {
            return new InMemoryOrganizationManagementService(List.of(
                    new OrganizationView("KNUE", "한국교원대학교", "UNIVERSITY", "ACTIVE", null),
                    new OrganizationView("COLLEGE", "사범대학", "COLLEGE", "ACTIVE", "KNUE"),
                    new OrganizationView("GRAD", "교육대학원", "GRADUATE_SCHOOL", "ACTIVE", "KNUE"),
                    new OrganizationView("CS", "컴퓨터교육과", "DEPARTMENT", "ACTIVE", "COLLEGE")),
                    List.of(new OrganizationRelationHistoryView("CS", "COLLEGE", LocalDate.parse("2020-01-01"), null, "ACTIVE")));
        }
    }
}
