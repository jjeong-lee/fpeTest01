package kr.ac.knue.achievement.menus;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisMenuPermissionManagementService implements MenuPermissionManagementService {
    private final MenuPermissionManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisMenuPermissionManagementService(MenuPermissionManagementMapper mapper,
            AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public MenuPermissionSearchResult search(String subjectType, String subjectId, int page, int size) {
        return new MenuPermissionSearchResult(mapper.search(subjectType, subjectId, size, page * size).stream()
                .map(this::view).toList(), mapper.count(subjectType, subjectId), page, size);
    }

    @Override
    public MenuPermissionView save(MenuPermissionRequest request, UUID actorUserId) {
        validateSubject(request.subjectType(), request.subjectId());
        UUID menuId = parseMenuId(request.menuId());
        validateDecision(request.accessDecision());
        if (!mapper.menuExists(menuId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
        }
        MenuPermissionManagementMapper.MenuPermissionRow current = mapper.findByNaturalKey(request.subjectType(),
                request.subjectId(), menuId);
        String beforeValue = current == null ? "null" : permissionJson(current);
        String afterValue = permissionJson(new MenuPermissionManagementMapper.MenuPermissionRow(request.subjectType(),
                request.subjectId(), menuId, null, null, null, request.accessDecision()));
        auditedCommandExecutor.execute(actorUserId, "menu_permission", request.subjectType() + ":"
                + request.subjectId() + ":" + menuId, beforeValue, afterValue, request.reason(), () -> {
                    if (current == null) mapper.insert(UUID.randomUUID(), request.subjectType(), request.subjectId(), menuId,
                            request.accessDecision());
                    else mapper.update(request.subjectType(), request.subjectId(), menuId, request.accessDecision());
                });
        return view(requirePermission(request.subjectType(), request.subjectId(), menuId));
    }

    private MenuPermissionManagementMapper.MenuPermissionRow requirePermission(String subjectType, String subjectId, UUID menuId) {
        MenuPermissionManagementMapper.MenuPermissionRow permission = mapper.findByNaturalKey(subjectType, subjectId, menuId);
        if (permission == null) throw new ApiException(HttpStatus.NOT_FOUND, "MENU_PERMISSION_NOT_FOUND", "메뉴 권한을 찾을 수 없습니다.");
        return permission;
    }

    private void validateSubject(String subjectType, String subjectId) {
        if (!"ROLE".equals(subjectType) && !"ORGANIZATION".equals(subjectType) && !"USER".equals(subjectType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SUBJECT_TYPE", "권한 대상 유형을 확인해 주세요.");
        }
        if (subjectId == null || subjectId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SUBJECT_ID", "권한 대상을 확인해 주세요.");
        }
    }

    private UUID parseMenuId(String menuId) {
        try { return UUID.fromString(menuId); }
        catch (IllegalArgumentException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MENU_ID", "메뉴 식별자 형식이 올바르지 않습니다."); }
    }

    private void validateDecision(String decision) {
        if (!"ALLOW".equals(decision) && !"DENY".equals(decision)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ACCESS_DECISION", "접근 허용 여부를 확인해 주세요.");
        }
    }

    private MenuPermissionView view(MenuPermissionManagementMapper.MenuPermissionRow row) {
        return new MenuPermissionView(row.subjectType(), row.subjectId(), row.menuId().toString(), row.topMenuName(),
                row.middleMenuName(), row.menuName(), row.accessDecision());
    }

    private String permissionJson(MenuPermissionManagementMapper.MenuPermissionRow row) {
        return "{\"subjectType\":\"" + json(row.subjectType()) + "\",\"subjectId\":\"" + json(row.subjectId())
                + "\",\"menuId\":\"" + row.menuId() + "\",\"accessDecision\":\"" + json(row.accessDecision()) + "\"}";
    }

    private String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
