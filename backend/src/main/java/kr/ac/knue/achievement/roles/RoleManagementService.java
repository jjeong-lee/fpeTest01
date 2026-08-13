package kr.ac.knue.achievement.roles;

import java.util.List;
import java.util.UUID;

public interface RoleManagementService {
    RoleSearchResult search(int page, int size);
    RoleView update(String roleCode, RoleUpdateRequest request, UUID actorUserId);

    record RoleSearchResult(List<RoleView> content, long totalElements, int page, int size) { }
}