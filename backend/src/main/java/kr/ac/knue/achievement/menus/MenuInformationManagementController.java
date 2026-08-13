package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiError.FieldError;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.common.CommandValidationException;
import kr.ac.knue.achievement.common.CommandValidator;

@RestController
public class MenuInformationManagementController {
    private static final Set<String> EXECUTION_INFORMATION_FIELDS = Set.of("menuName", "screenId", "url", "icon", "businessCategory", "description", "reason");

    private final MenuInformationManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public MenuInformationManagementController(MenuInformationManagementService service,
            AuthenticationPort authenticationPort, CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/menus")
    public ApiResponse<List<MenuInformationView>> listMenus() {
        return ApiResponse.success(service.listMenus());
    }

    @PutMapping("/api/menus/{menuId}")
    public ApiResponse<MenuInformationView> updateMenu(@PathVariable UUID menuId,
            @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, EXECUTION_INFORMATION_FIELDS);
        MenuInformationUpdateRequest update = new MenuInformationUpdateRequest(
                requiredString(command, "menuName"), requiredString(command, "screenId"),
                requiredString(command, "url"), optionalString(command, "icon"),
                optionalString(command, "businessCategory"), optionalString(command, "description"),
                requiredString(command, "reason"));
        return ApiResponse.success(service.updateMenu(menuId, update, actorId(request)));
    }

    private String requiredString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw validation(field, "필수 입력 항목입니다.");
        return text.trim();
    }

    private String optionalString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (value == null) return null;
        if (!(value instanceof String text)) throw validation(field, "문자열 형식이어야 합니다.");
        return text.trim();
    }

    private UUID actorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) {
            AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue());
            if (session != null) return session.userId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }

    private CommandValidationException validation(String field, String message) {
        return new CommandValidationException(new FieldError(field, message));
    }
}
