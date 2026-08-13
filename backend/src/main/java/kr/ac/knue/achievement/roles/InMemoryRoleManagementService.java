package kr.ac.knue.achievement.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import kr.ac.knue.achievement.common.ApiException;

public class InMemoryRoleManagementService implements RoleManagementService {
    private final List<RoleView> roles = new ArrayList<>(List.of(
            new RoleView("R01", "교원", "본인 관련 업무를 수행한다.", null, null, "ACTIVE"),
            new RoleView("R02", "학과장", "소속 학과 교원 관련 업무를 확인한다.", null, null, "ACTIVE"),
            new RoleView("R03", "단과대학(원) 행정실", "단과대학 또는 대학원 행정을 처리한다.", null, null, "ACTIVE"),
            new RoleView("R04", "교수지원과", "기준정보와 평가 관련 행정을 관리한다.", null, null, "ACTIVE"),
            new RoleView("R05", "산학협력단", "연구비 관련 자료를 관리한다.", null, null, "ACTIVE"),
            new RoleView("R06", "입학인재관리과", "입학 및 취업률 관련 자료를 관리한다.", null, null, "ACTIVE"),
            new RoleView("R07", "실적부서", "담당 실적 자료를 관리한다.", null, null, "ACTIVE"),
            new RoleView("R08", "점수산출 감사자", "산출 과정과 근거를 조회한다.", null, null, "ACTIVE"),
            new RoleView("R09", "시스템관리자", "사용자, 조직, 메뉴, 권한, 코드를 관리한다.", null, null, "ACTIVE")));

    @Override
    public RoleSearchResult search(int page, int size) {
        int from = Math.min(page * size, roles.size());
        int to = Math.min(from + size, roles.size());
        return new RoleSearchResult(List.copyOf(roles.subList(from, to)), roles.size(), page, size);
    }

    @Override
    public RoleView update(String roleCode, RoleUpdateRequest request, UUID actorUserId) {
        RoleView current = find(roleCode);
        if (request.useStatus() != null && !"ACTIVE".equals(request.useStatus()) && !"INACTIVE".equals(request.useStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USE_STATUS", "사용 상태를 확인해 주세요.");
        }
        RoleView updated = new RoleView(roleCode, request.roleName() == null ? current.roleName() : request.roleName(),
                current.purpose(), request.grantCriteria() == null ? current.grantCriteria() : request.grantCriteria(),
                request.dataScopeDefault() == null ? current.dataScopeDefault() : request.dataScopeDefault(),
                request.useStatus() == null ? current.useStatus() : request.useStatus());
        roles.set(roles.indexOf(current), updated);
        return updated;
    }

    private RoleView find(String roleCode) {
        return roles.stream().filter(role -> role.roleCode().equals(roleCode)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."));
    }
}