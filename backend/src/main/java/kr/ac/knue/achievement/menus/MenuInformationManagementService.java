package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

public interface MenuInformationManagementService {
    List<MenuInformationView> listMenus();
    MenuInformationView updateMenu(UUID menuId, MenuInformationUpdateRequest request, UUID actorUserId);
}
