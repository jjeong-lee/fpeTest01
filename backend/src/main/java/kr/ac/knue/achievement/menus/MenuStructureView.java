package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

public record MenuStructureView(UUID menuId, UUID parentMenuId, String menuName, int displayOrder,
        String useStatus, List<MenuStructureView> children) { }
