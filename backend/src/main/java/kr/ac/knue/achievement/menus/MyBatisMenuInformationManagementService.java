package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisMenuInformationManagementService implements MenuInformationManagementService {
    private final MenuInformationManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisMenuInformationManagementService(MenuInformationManagementMapper mapper,
            AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public List<MenuInformationView> listMenus() {
        return mapper.findActiveMenus().stream().map(this::view).toList();
    }

    @Override
    public MenuInformationView updateMenu(UUID menuId, MenuInformationUpdateRequest request, UUID actorUserId) {
        MenuInformationManagementMapper.MenuInformationRow current = requireMenu(menuId);
        auditedCommandExecutor.execute(actorUserId, "menu", menuId.toString(), json(current), json(menuId, request),
                request.reason(), () -> mapper.updateExecutionInformation(menuId, request.menuName(), request.screenId(),
                        request.url(), request.icon(), request.businessCategory(), request.description()));
        return view(requireMenu(menuId));
    }

    private MenuInformationManagementMapper.MenuInformationRow requireMenu(UUID menuId) {
        MenuInformationManagementMapper.MenuInformationRow menu = mapper.findActiveMenu(menuId);
        if (menu == null) throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
        return menu;
    }

    private MenuInformationView view(MenuInformationManagementMapper.MenuInformationRow row) {
        return new MenuInformationView(row.menuId(), row.menuName(), row.screenId(), row.url(), row.icon(),
                row.businessCategory(), row.description(), row.useStatus());
    }

    private String json(MenuInformationManagementMapper.MenuInformationRow row) {
        return json(row.menuId(), new MenuInformationUpdateRequest(row.menuName(), row.screenId(), row.url(), row.icon(),
                row.businessCategory(), row.description(), ""));
    }

    private String json(UUID menuId, MenuInformationUpdateRequest request) {
        return "{\"menuId\":\"" + menuId + "\",\"menuName\":\"" + escape(request.menuName())
                + "\",\"screenId\":\"" + escape(request.screenId()) + "\",\"url\":\"" + escape(request.url())
                + "\",\"icon\":" + nullable(request.icon()) + ",\"businessCategory\":"
                + nullable(request.businessCategory()) + ",\"description\":" + nullable(request.description()) + "}";
    }

    private String nullable(String value) { return value == null ? "null" : "\"" + escape(value) + "\""; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
