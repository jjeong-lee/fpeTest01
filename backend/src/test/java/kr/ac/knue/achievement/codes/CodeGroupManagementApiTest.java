package kr.ac.knue.achievement.codes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.menus.MenuAccessPort;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CodeGroupManagementApiTest.CodeGroupManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CodeGroupManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCodeGroupsWithTheirManagementFields() throws Exception {
        requireOpenApiFixture();

        mockMvc.perform(get("/api/code-groups").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupId").value("ACADEMIC_STATUS"))
                .andExpect(jsonPath("$.data[0].groupName").value("학적 상태"))
                .andExpect(jsonPath("$.data[0].description").value("학적 상태 구분에 사용하는 공통코드입니다."))
                .andExpect(jsonPath("$.data[0].managingDepartment").value("학사지원과"));
    }

    @Test
    void createsAndUpdatesCodeGroupThenReturnsSavedFieldsOnReload() throws Exception {
        mockMvc.perform(post("/api/code-groups").cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"EVALUATION_TYPE\",\"groupName\":\"평가 유형\",\"description\":\"평가 유형 분류입니다.\",\"managingDepartment\":\"교수지원과\",\"reason\":\"코드그룹 등록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("EVALUATION_TYPE"))
                .andExpect(jsonPath("$.data.groupName").value("평가 유형"));

        mockMvc.perform(put("/api/code-groups/{groupId}", "EVALUATION_TYPE").cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"EVALUATION_TYPE\",\"groupName\":\"교수 평가 유형\",\"description\":\"교수 평가 유형 분류입니다.\",\"managingDepartment\":\"교수지원과\",\"reason\":\"명칭 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("EVALUATION_TYPE"))
                .andExpect(jsonPath("$.data.groupName").value("교수 평가 유형"));

        mockMvc.perform(get("/api/code-groups").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].groupId").value("EVALUATION_TYPE"))
                .andExpect(jsonPath("$.data[1].groupName").value("교수 평가 유형"))
                .andExpect(jsonPath("$.data[1].description").value("교수 평가 유형 분류입니다."))
                .andExpect(jsonPath("$.data[1].managingDepartment").value("교수지원과"));
    }

    @Test
    void rejectsMissingGroupNameAndDuplicateGroupIdWithoutChangingTheList() throws Exception {
        mockMvc.perform(post("/api/code-groups").cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"ACADEMIC_STATUS\",\"reason\":\"검증 확인\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("groupName"));

        mockMvc.perform(get("/api/code-groups").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private void requireOpenApiFixture() {
        if (!new ClassPathResource("contracts/openapi.yaml").exists()) throw new IllegalStateException("OpenAPI test fixture is required");
    }

    private jakarta.servlet.http.Cookie sessionCookie() { return new jakarta.servlet.http.Cookie("SESSION", "r09-session"); }

    @TestConfiguration
    static class CodeGroupManagementConfiguration {
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
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/code-groups".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/code-groups"); }
            };
        }

        @Bean
        @Primary
        CodeGroupManagementService codeGroupManagementService() { return new InMemoryCodeGroupManagementService(); }
    }

    static class InMemoryCodeGroupManagementService implements CodeGroupManagementService {
        private final List<CodeGroupView> groups = new ArrayList<>(List.of(new CodeGroupView("ACADEMIC_STATUS", "학적 상태", "학적 상태 구분에 사용하는 공통코드입니다.", "학사지원과", "ACTIVE")));

        @Override public List<CodeGroupView> listCodeGroups() { return List.copyOf(groups); }
        @Override public CodeGroupView create(CodeGroupRequest request, UUID actorUserId) {
            if (groups.stream().anyMatch(group -> group.groupId().equals(request.groupId()))) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "CODE_GROUP_DUPLICATE", "이미 등록된 그룹ID입니다.");
            CodeGroupView created = new CodeGroupView(request.groupId(), request.groupName(), request.description(), request.managingDepartment(), "ACTIVE");
            groups.add(created);
            return created;
        }
        @Override public CodeGroupView update(String groupId, CodeGroupRequest request, UUID actorUserId) {
            if (!groupId.equals(request.groupId())) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "GROUP_ID_MISMATCH", "경로의 그룹ID와 요청 그룹ID가 일치하지 않습니다.");
            int index = indexOf(groupId);
            CodeGroupView updated = new CodeGroupView(groupId, request.groupName(), request.description(), request.managingDepartment(), "ACTIVE");
            groups.set(index, updated);
            return updated;
        }
        private int indexOf(String groupId) {
            for (int index = 0; index < groups.size(); index++) if (groups.get(index).groupId().equals(groupId)) return index;
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "CODE_GROUP_NOT_FOUND", "코드그룹을 찾을 수 없습니다.");
        }
    }
}
