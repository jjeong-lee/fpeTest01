package kr.ac.knue.achievement.organizations;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisOrganizationManagementService implements OrganizationManagementService {
    private final OrganizationManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisOrganizationManagementService(OrganizationManagementMapper mapper,
            AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public OrganizationSearchResult search(String organizationCode, int page, int size) {
        return new OrganizationSearchResult(mapper.search(organizationCode, size, page * size).stream().map(this::view).toList(),
                mapper.count(organizationCode), page, size);
    }

    @Override
    public OrganizationTreeView tree(String organizationCode) {
        OrganizationManagementMapper.OrganizationRow selected = requireOrganization(organizationCode);
        LocalDate today = LocalDate.now();
        OrganizationManagementMapper.OrganizationRow parent = mapper.findEffectiveParent(organizationCode, today);
        List<OrganizationView> children = mapper.findEffectiveChildren(organizationCode, today).stream().map(this::view).toList();
        return new OrganizationTreeView(selected.organizationCode(), selected.organizationName(), selected.organizationType(),
                selected.useStatus(), parent == null ? null : view(parent), children,
                mapper.findHistory(organizationCode).stream().map(this::historyView).toList());
    }

    @Override
    public OrganizationTreeView saveRelation(String organizationCode, OrganizationRelationRequest request, UUID actorUserId) {
        requireOrganization(organizationCode);
        String parentOrganizationCode = blankToNull(request.parentOrganizationCode());
        if (organizationCode.equals(parentOrganizationCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT_ORGANIZATION", "상위조직은 대상 조직과 같을 수 없습니다.");
        }
        if (parentOrganizationCode != null) requireOrganization(parentOrganizationCode);
        if (request.effectiveEndDate() != null && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EFFECTIVE_PERIOD", "적용 종료일은 적용 시작일보다 빠를 수 없습니다.");
        }
        LocalDate today = LocalDate.now();
        OrganizationManagementMapper.RelationRow current = mapper.findActiveRelation(organizationCode, today);
        if (current != null && !request.effectiveStartDate().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RELATION_PERIOD_CONFLICT", "기존 유효 관계를 침범하는 적용기간은 저장할 수 없습니다.");
        }
        String beforeValue = current == null ? "null" : relationJson(current);
        String afterValue = relationJson(organizationCode, parentOrganizationCode, request.effectiveStartDate(), request.effectiveEndDate());
        OrganizationManagementMapper.RelationRow relationToClose = current;
        auditedCommandExecutor.execute(actorUserId, "organization_relation_history", organizationCode, beforeValue, afterValue,
                request.reason(), () -> {
                    if (relationToClose != null) mapper.closeRelation(relationToClose.relationId(), request.effectiveStartDate().minusDays(1));
                    mapper.insertRelation(UUID.randomUUID(), organizationCode, parentOrganizationCode, request.effectiveStartDate(), request.effectiveEndDate());
                });
        return tree(organizationCode);
    }

    private OrganizationManagementMapper.OrganizationRow requireOrganization(String organizationCode) {
        OrganizationManagementMapper.OrganizationRow row = mapper.findByCode(organizationCode);
        if (row == null) throw new ApiException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "조직을 찾을 수 없습니다.");
        return row;
    }

    private OrganizationView view(OrganizationManagementMapper.OrganizationRow row) {
        return new OrganizationView(row.organizationCode(), row.organizationName(), row.organizationType(), row.useStatus(),
                row.currentParentOrganizationCode());
    }

    private OrganizationRelationHistoryView historyView(OrganizationManagementMapper.RelationRow row) {
        return new OrganizationRelationHistoryView(row.organizationCode(), row.parentOrganizationCode(), row.effectiveStartDate(),
                row.effectiveEndDate(), row.status());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private String relationJson(OrganizationManagementMapper.RelationRow row) {
        return relationJson(row.organizationCode(), row.parentOrganizationCode(), row.effectiveStartDate(), row.effectiveEndDate());
    }

    private String relationJson(String organizationCode, String parentOrganizationCode, LocalDate startDate, LocalDate endDate) {
        return "{\"organizationCode\":\"" + organizationCode + "\",\"parentOrganizationCode\":"
                + (parentOrganizationCode == null ? "null" : "\"" + parentOrganizationCode + "\"")
                + ",\"effectiveStartDate\":\"" + startDate + "\",\"effectiveEndDate\":"
                + (endDate == null ? "null" : "\"" + endDate + "\"") + "}";
    }
}
