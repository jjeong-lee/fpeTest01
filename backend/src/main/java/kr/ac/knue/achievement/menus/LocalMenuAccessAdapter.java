package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class LocalMenuAccessAdapter implements MenuAccessPort {
    private final MenuAuthorizationMapper mapper;

    public LocalMenuAccessAdapter(MenuAuthorizationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean mayAccess(UUID userId, String organizationCode, Set<String> roleCodes, String menuPath) {
        return allowedMenuPaths(userId, organizationCode, roleCodes).contains(menuPath);
    }

    @Override
    public List<String> allowedMenuPaths(UUID userId, String organizationCode, Set<String> roleCodes) {
        return mapper.findAllowedMenuPaths(userId, organizationCode);
    }
}
