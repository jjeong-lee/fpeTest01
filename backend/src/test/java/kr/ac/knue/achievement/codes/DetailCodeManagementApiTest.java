package kr.ac.knue.achievement.codes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpStatus;
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
@Import(DetailCodeManagementApiTest.DetailCodeManagementConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DetailCodeManagementApiTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID PARENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID CHILD_ID = UUID.fromString("00000000-0000-0000-0000-000000000902");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsOnlyTheSelectedGroupsDetailCodesWithHierarchyAndAttributes() throws Exception {
        requireOpenApiFixture();

        mockMvc.perform(get("/api/code-groups/{groupId}/detail-codes", "ACADEMIC_STATUS").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].detailCodeId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].codeValue").value("ENROLLED"))
                .andExpect(jsonPath("$.data[0].parentDetailCodeId").doesNotExist())
                .andExpect(jsonPath("$.data[1].parentDetailCodeId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.data[1].additionalAttributes.linkedCode").value("STUDENT_STATUS"));
    }

    @Test
    void createsAndUpdatesDetailCodeThenReloadsItsParentAndAdditionalAttributes() throws Exception {
        UUID createdId = UUID.fromString("00000000-0000-0000-0000-000000000903");
        mockMvc.perform(post("/api/detail-codes").cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"ACADEMIC_STATUS\",\"codeValue\":\"LEAVE\",\"codeName\":\"휴학\",\"parentDetailCodeId\":\"" + PARENT_ID + "\",\"displayOrder\":3,\"additionalAttributes\":{\"linkedCode\":\"LEAVE_TYPE\"},\"reason\":\"상세코드 등록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codeValue").value("LEAVE"))
                .andExpect(jsonPath("$.data.parentDetailCodeId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.data.additionalAttributes.linkedCode").value("LEAVE_TYPE"));

        mockMvc.perform(put("/api/detail-codes/{detailCodeId}", createdId).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"ACADEMIC_STATUS\",\"codeValue\":\"LEAVE\",\"codeName\":\"일반 휴학\",\"parentDetailCodeId\":\"" + PARENT_ID + "\",\"displayOrder\":4,\"additionalAttributes\":{\"linkedCode\":\"GENERAL_LEAVE\"},\"reason\":\"상세코드 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codeName").value("일반 휴학"))
                .andExpect(jsonPath("$.data.displayOrder").value(4));

        mockMvc.perform(get("/api/code-groups/{groupId}/detail-codes", "ACADEMIC_STATUS").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[2].detailCodeId").value(createdId.toString()))
                .andExpect(jsonPath("$.data[2].parentDetailCodeId").value(PARENT_ID.toString()))
                .andExpect(jsonPath("$.data[2].additionalAttributes.linkedCode").value("GENERAL_LEAVE"));
    }

    @Test
    void rejectsDuplicateCodeValueAndCrossGroupParentWithoutChangingDetailCodes() throws Exception {
        mockMvc.perform(post("/api/detail-codes").cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"ACADEMIC_STATUS\",\"codeValue\":\"ENROLLED\",\"codeName\":\"중복\",\"displayOrder\":3,\"reason\":\"중복 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DETAIL_CODE_DUPLICATE"));

        mockMvc.perform(put("/api/detail-codes/{detailCodeId}", CHILD_ID).cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"ACADEMIC_STATUS\",\"codeValue\":\"ACTIVE\",\"codeName\":\"재학\",\"parentDetailCodeId\":\"00000000-0000-0000-0000-000000000999\",\"displayOrder\":2,\"reason\":\"상위코드 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DETAIL_CODE_PARENT_INVALID"));

        mockMvc.perform(get("/api/code-groups/{groupId}/detail-codes", "ACADEMIC_STATUS").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private void requireOpenApiFixture() {
        if (!new ClassPathResource("contracts/openapi.yaml").exists()) throw new IllegalStateException("OpenAPI test fixture is required");
    }

    private jakarta.servlet.http.Cookie sessionCookie() { return new jakarta.servlet.http.Cookie("SESSION", "r09-session"); }

    @TestConfiguration
    static class DetailCodeManagementConfiguration {
        @Bean
        AuthenticationPort authenticationPort() {
            return new AuthenticationPort() {
                private final AuthenticatedSession session = new AuthenticatedSession(ADMIN_ID, "r09-session", "KNUE", "admin", Set.of("R09"), Instant.now().plusSeconds(3600));
                @Override public LoginResult login(String username, String password) { return LoginResult.failure(); }
                @Override public AuthenticatedSession findActiveSession(String sessionId) { return "r09-session".equals(sessionId) ? session : null; }
                @Override public boolean logout(String sessionId) { return false; }
            };
        }

        @Bean
        MenuAccessPort menuAccessPort() {
            return new MenuAccessPort() {
                @Override public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) { return roleCodes.contains("R09") && "/system/detail-codes".equals(menuPath); }
                @Override public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) { return List.of("/system/detail-codes"); }
            };
        }

        @Bean
        @Primary
        DetailCodeManagementService detailCodeManagementService() { return new InMemoryDetailCodeManagementService(); }
    }

    static class InMemoryDetailCodeManagementService implements DetailCodeManagementService {
        private final List<DetailCodeView> codes = new ArrayList<>(List.of(
                new DetailCodeView(PARENT_ID, "ACADEMIC_STATUS", "ENROLLED", "재학", null, 1, Map.of(), "ACTIVE"),
                new DetailCodeView(CHILD_ID, "ACADEMIC_STATUS", "ACTIVE", "재학", PARENT_ID, 2, Map.of("linkedCode", "STUDENT_STATUS"), "ACTIVE")));

        @Override public List<DetailCodeView> listByGroup(String groupId) { return codes.stream().filter(code -> code.groupId().equals(groupId)).toList(); }
        @Override public DetailCodeView create(DetailCodeRequest request, UUID actorUserId) {
            if (codes.stream().anyMatch(code -> code.groupId().equals(request.groupId()) && code.codeValue().equals(request.codeValue()))) throw new ApiException(HttpStatus.BAD_REQUEST, "DETAIL_CODE_DUPLICATE", "같은 그룹에 이미 등록된 코드값입니다.");
            validateParent(request, null);
            DetailCodeView created = new DetailCodeView(UUID.fromString("00000000-0000-0000-0000-000000000903"), request.groupId(), request.codeValue(), request.codeName(), request.parentDetailCodeId(), request.displayOrder(), request.additionalAttributes(), "ACTIVE");
            codes.add(created);
            return created;
        }
        @Override public DetailCodeView update(UUID detailCodeId, DetailCodeRequest request, UUID actorUserId) {
            int index = indexOf(detailCodeId);
            validateParent(request, detailCodeId);
            DetailCodeView updated = new DetailCodeView(detailCodeId, request.groupId(), request.codeValue(), request.codeName(), request.parentDetailCodeId(), request.displayOrder(), request.additionalAttributes(), "ACTIVE");
            codes.set(index, updated);
            return updated;
        }
        private void validateParent(DetailCodeRequest request, UUID detailCodeId) {
            if (request.parentDetailCodeId() != null && (request.parentDetailCodeId().equals(detailCodeId) || codes.stream().noneMatch(code -> code.detailCodeId().equals(request.parentDetailCodeId()) && code.groupId().equals(request.groupId())))) throw new ApiException(HttpStatus.BAD_REQUEST, "DETAIL_CODE_PARENT_INVALID", "상위코드는 같은 그룹의 다른 상세코드여야 합니다.");
        }
        private int indexOf(UUID detailCodeId) {
            for (int index = 0; index < codes.size(); index++) if (codes.get(index).detailCodeId().equals(detailCodeId)) return index;
            throw new ApiException(HttpStatus.NOT_FOUND, "DETAIL_CODE_NOT_FOUND", "상세코드를 찾을 수 없습니다.");
        }
    }
}
