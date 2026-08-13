package kr.ac.knue.achievement.menus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiError.FieldError;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;
import kr.ac.knue.achievement.common.CommandValidationException;

@Service
@Profile("!test")
public class MyBatisMenuStructureManagementService implements MenuStructureManagementService {
    private final MenuStructureManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisMenuStructureManagementService(MenuStructureManagementMapper mapper, AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public List<MenuStructureView> structure() {
        return tree(mapper.findActiveMenus());
    }

    @Override
    public MenuStructureView updateParent(UUID menuId, MenuParentRequest request, UUID actorUserId) {
        MenuStructureManagementMapper.MenuRow current = requireMenu(menuId);
        UUID parentMenuId = request.parentMenuId();
        if (parentMenuId == null) throw validation("parentMenuId", "부모메뉴를 선택하세요.");
        if (menuId.equals(parentMenuId)) throw validation("parentMenuId", "자기 자신을 부모메뉴로 지정할 수 없습니다.");
        requireMenu(parentMenuId);
        if (descendantIds(menuId).contains(parentMenuId)) throw validation("parentMenuId", "하위 메뉴를 부모메뉴로 지정할 수 없습니다.");
        auditedCommandExecutor.execute(actorUserId, "menu", menuId.toString(), json(current),
                json(menuId, parentMenuId, current.menuName(), current.displayOrder(), current.useStatus()), request.reason(),
                () -> mapper.updateParent(menuId, parentMenuId));
        return view(requireMenu(menuId), List.of());
    }

    @Override
    public MenuStructureView updateDisplayOrder(UUID menuId, MenuDisplayOrderRequest request, UUID actorUserId) {
        MenuStructureManagementMapper.MenuRow current = requireMenu(menuId);
        if (request.displayOrder() == null || request.displayOrder() < 0) throw validation("displayOrder", "표시순서는 0 이상의 값이어야 합니다.");
        auditedCommandExecutor.execute(actorUserId, "menu", menuId.toString(), json(current),
                json(menuId, current.parentMenuId(), current.menuName(), request.displayOrder(), current.useStatus()), request.reason(),
                () -> mapper.updateDisplayOrder(menuId, request.displayOrder()));
        return view(requireMenu(menuId), List.of());
    }

    private MenuStructureManagementMapper.MenuRow requireMenu(UUID menuId) {
        MenuStructureManagementMapper.MenuRow menu = mapper.findActiveMenu(menuId);
        if (menu == null) throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
        return menu;
    }

    private Set<UUID> descendantIds(UUID rootMenuId) {
        Map<UUID, List<UUID>> children = new HashMap<>();
        for (MenuStructureManagementMapper.MenuRow menu : mapper.findActiveMenus()) {
            if (menu.parentMenuId() != null) children.computeIfAbsent(menu.parentMenuId(), ignored -> new ArrayList<>()).add(menu.menuId());
        }
        Set<UUID> descendants = new HashSet<>();
        collectDescendants(rootMenuId, children, descendants);
        return descendants;
    }

    private void collectDescendants(UUID menuId, Map<UUID, List<UUID>> children, Set<UUID> descendants) {
        for (UUID childId : children.getOrDefault(menuId, List.of())) if (descendants.add(childId)) collectDescendants(childId, children, descendants);
    }

    private List<MenuStructureView> tree(List<MenuStructureManagementMapper.MenuRow> menus) {
        Map<UUID, List<MenuStructureManagementMapper.MenuRow>> children = new HashMap<>();
        for (MenuStructureManagementMapper.MenuRow menu : menus) children.computeIfAbsent(menu.parentMenuId(), ignored -> new ArrayList<>()).add(menu);
        return buildTree(null, children);
    }

    private List<MenuStructureView> buildTree(UUID parentMenuId, Map<UUID, List<MenuStructureManagementMapper.MenuRow>> children) {
        return children.getOrDefault(parentMenuId, List.of()).stream().sorted(Comparator.comparingInt(MenuStructureManagementMapper.MenuRow::displayOrder)
                .thenComparing(MenuStructureManagementMapper.MenuRow::menuName)).map(menu -> view(menu, buildTree(menu.menuId(), children))).toList();
    }

    private MenuStructureView view(MenuStructureManagementMapper.MenuRow menu, List<MenuStructureView> children) {
        return new MenuStructureView(menu.menuId(), menu.parentMenuId(), menu.menuName(), menu.displayOrder(), menu.useStatus(), children);
    }

    private CommandValidationException validation(String field, String message) { return new CommandValidationException(new FieldError(field, message)); }

    private String json(MenuStructureManagementMapper.MenuRow menu) { return json(menu.menuId(), menu.parentMenuId(), menu.menuName(), menu.displayOrder(), menu.useStatus()); }
    private String json(UUID id, UUID parentId, String name, int order, String status) {
        return "{\"menuId\":\"" + id + "\",\"parentMenuId\":" + (parentId == null ? "null" : "\"" + parentId + "\"")
                + ",\"menuName\":\"" + escape(name) + "\",\"displayOrder\":" + order + ",\"useStatus\":\"" + escape(status) + "\"}";
    }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
