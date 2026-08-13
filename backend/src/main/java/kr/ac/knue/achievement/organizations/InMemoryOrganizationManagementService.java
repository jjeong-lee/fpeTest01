package kr.ac.knue.achievement.organizations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import kr.ac.knue.achievement.common.ApiException;

public class InMemoryOrganizationManagementService implements OrganizationManagementService {
    private final List<OrganizationView> organizations;
    private final List<OrganizationRelationHistoryView> history;

    public InMemoryOrganizationManagementService(List<OrganizationView> organizations,
            List<OrganizationRelationHistoryView> history) {
        this.organizations = new ArrayList<>(organizations);
        this.history = new ArrayList<>(history);
    }

    @Override
    public OrganizationSearchResult search(String organizationCode, int page, int size) {
        List<OrganizationView> matches = organizations.stream()
                .filter(organization -> organizationCode == null || organizationCode.isBlank()
                        || organization.organizationCode().contains(organizationCode))
                .map(organization -> new OrganizationView(organization.organizationCode(), organization.organizationName(),
                        organization.organizationType(), organization.useStatus(), currentParentCode(organization.organizationCode(), LocalDate.now())))
                .sorted(Comparator.comparing(OrganizationView::organizationCode))
                .toList();
        int from = Math.min(page * size, matches.size());
        int to = Math.min(from + size, matches.size());
        return new OrganizationSearchResult(matches.subList(from, to), matches.size(), page, size);
    }

    @Override
    public OrganizationTreeView tree(String organizationCode) {
        OrganizationView selected = findOrganization(organizationCode);
        OrganizationView parent = currentParentCode(organizationCode, LocalDate.now()) == null ? null
                : findOrganization(currentParentCode(organizationCode, LocalDate.now()));
        List<OrganizationView> children = organizations.stream()
                .filter(organization -> organizationCode.equals(currentParentCode(organization.organizationCode(), LocalDate.now())))
                .sorted(Comparator.comparing(OrganizationView::organizationCode))
                .toList();
        return new OrganizationTreeView(selected.organizationCode(), selected.organizationName(), selected.organizationType(),
                selected.useStatus(), parent, children, historyFor(organizationCode));
    }

    @Override
    public OrganizationTreeView saveRelation(String organizationCode, OrganizationRelationRequest request, UUID actorUserId) {
        findOrganization(organizationCode);
        if (request.parentOrganizationCode() != null && !request.parentOrganizationCode().isBlank()) {
            if (organizationCode.equals(request.parentOrganizationCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT_ORGANIZATION", "상위조직은 대상 조직과 같을 수 없습니다.");
            }
            findOrganization(request.parentOrganizationCode());
        }
        if (request.effectiveEndDate() != null && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EFFECTIVE_PERIOD", "적용 종료일은 적용 시작일보다 빠를 수 없습니다.");
        }
        LocalDate today = LocalDate.now();
        OrganizationRelationHistoryView current = activeRelation(organizationCode, today);
        if (current != null && !request.effectiveStartDate().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RELATION_PERIOD_CONFLICT", "기존 유효 관계를 침범하는 적용기간은 저장할 수 없습니다.");
        }
        if (current != null && (current.effectiveEndDate() == null || !current.effectiveEndDate().isBefore(request.effectiveStartDate()))) {
            history.remove(current);
            history.add(new OrganizationRelationHistoryView(current.organizationCode(), current.parentOrganizationCode(),
                    current.effectiveStartDate(), request.effectiveStartDate().minusDays(1), current.status()));
        }
        history.add(new OrganizationRelationHistoryView(organizationCode, blankToNull(request.parentOrganizationCode()),
                request.effectiveStartDate(), request.effectiveEndDate(), "ACTIVE"));
        return tree(organizationCode);
    }

    private OrganizationView findOrganization(String organizationCode) {
        return organizations.stream().filter(organization -> organization.organizationCode().equals(organizationCode)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "조직을 찾을 수 없습니다."));
    }

    private OrganizationRelationHistoryView activeRelation(String organizationCode, LocalDate onDate) {
        return history.stream().filter(relation -> relation.organizationCode().equals(organizationCode)
                && "ACTIVE".equals(relation.status()) && !relation.effectiveStartDate().isAfter(onDate)
                && (relation.effectiveEndDate() == null || !relation.effectiveEndDate().isBefore(onDate)))
                .max(Comparator.comparing(OrganizationRelationHistoryView::effectiveStartDate)).orElse(null);
    }

    private String currentParentCode(String organizationCode, LocalDate onDate) {
        OrganizationRelationHistoryView relation = activeRelation(organizationCode, onDate);
        if (relation != null) {
            return relation.parentOrganizationCode();
        }
        return findOrganization(organizationCode).currentParentOrganizationCode();
    }

    private List<OrganizationRelationHistoryView> historyFor(String organizationCode) {
        return history.stream().filter(relation -> relation.organizationCode().equals(organizationCode))
                .sorted(Comparator.comparing(OrganizationRelationHistoryView::effectiveStartDate)).toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
