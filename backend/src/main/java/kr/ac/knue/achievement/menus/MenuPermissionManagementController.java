package kr.ac.knue.achievement.menus;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiError.FieldError;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.common.CommandValidationException;
import kr.ac.knue.achievement.common.CommandValidator;

@RestController
public class MenuPermissionManagementController {
    private final MenuPermissionManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public MenuPermissionManagementController(MenuPermissionManagementService service, AuthenticationPort authenticationPort,
            CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/menu-permissions")
    public ApiResponse<MenuPermissionManagementService.MenuPermissionSearchResult> search(
            @RequestParam(required = false) String subjectType, @RequestParam(required = false) String subjectId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "페이지 정보를 확인해 주세요.");
        return ApiResponse.success(service.search(blankToNull(subjectType), blankToNull(subjectId), page, Math.min(size, 100)));
    }

    @PutMapping("/api/menu-permissions")
    public ApiResponse<MenuPermissionView> save(@RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("subjectType", "subjectId", "menuId", "accessDecision", "reason"));
        commandValidator.requireReason(command, "reason");
        return ApiResponse.success(service.save(new MenuPermissionRequest(requiredString(command, "subjectType"),
                requiredString(command, "subjectId"), requiredString(command, "menuId"),
                requiredString(command, "accessDecision"), (String) command.get("reason")), actorId(request)));
    }

    private String requiredString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new CommandValidationException(new FieldError(field, "필수 입력 항목입니다."));
        }
        return text;
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private UUID actorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) {
            AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue());
            if (session != null) return session.userId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
