package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MenuAccessPort {
    boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath);

    List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes);
}
