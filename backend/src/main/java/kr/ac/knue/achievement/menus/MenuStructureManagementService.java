package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

public interface MenuStructureManagementService {
    List<MenuStructureView> structure();

    MenuStructureView updateParent(UUID menuId, MenuParentRequest request, UUID actorUserId);

    MenuStructureView updateDisplayOrder(UUID menuId, MenuDisplayOrderRequest request, UUID actorUserId);
}
