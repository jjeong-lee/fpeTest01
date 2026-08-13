package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

public interface MenuPermissionManagementService {
    MenuPermissionSearchResult search(String subjectType, String subjectId, int page, int size);

    MenuPermissionView save(MenuPermissionRequest request, UUID actorUserId);

    record MenuPermissionSearchResult(List<MenuPermissionView> content, long totalElements, int page, int size) { }
}
