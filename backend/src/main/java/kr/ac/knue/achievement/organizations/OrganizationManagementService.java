package kr.ac.knue.achievement.organizations;

import java.util.List;
import java.util.UUID;

public interface OrganizationManagementService {
    OrganizationSearchResult search(String organizationCode, int page, int size);
    OrganizationTreeView tree(String organizationCode);
    OrganizationTreeView saveRelation(String organizationCode, OrganizationRelationRequest request, UUID actorUserId);

    record OrganizationSearchResult(List<OrganizationView> content, long totalElements, int page, int size) { }
}
