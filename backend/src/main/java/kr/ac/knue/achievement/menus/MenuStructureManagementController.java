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
public class MenuStructureManagementController {
    private final MenuStructureManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public MenuStructureManagementController(MenuStructureManagementService service, AuthenticationPort authenticationPort, CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/menu-structure")
    public ApiResponse<List<MenuStructureView>> structure() { return ApiResponse.success(service.structure()); }

    @PutMapping("/api/menus/{menuId}/parent")
    public ApiResponse<MenuStructureView> updateParent(@PathVariable UUID menuId, @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("parentMenuId", "reason"));
        commandValidator.requireReason(command, "reason");
        return ApiResponse.success(service.updateParent(menuId, new MenuParentRequest(requiredUuid(command, "parentMenuId"), requiredString(command, "reason")), actorId(request)));
    }

    @PutMapping("/api/menus/{menuId}/display-order")
    public ApiResponse<MenuStructureView> updateDisplayOrder(@PathVariable UUID menuId, @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("displayOrder", "reason"));
        commandValidator.requireReason(command, "reason");
        Object value = command.get("displayOrder");
        if (!(value instanceof Number number)) throw validation("displayOrder", "표시순서는 필수 입력 항목입니다.");
        return ApiResponse.success(service.updateDisplayOrder(menuId, new MenuDisplayOrderRequest(number.intValue(), requiredString(command, "reason")), actorId(request)));
    }

    private UUID requiredUuid(Map<String, Object> command, String field) {
        String value = requiredString(command, field);
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw validation(field, "식별자 형식이 올바르지 않습니다."); }
    }
    private String requiredString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw validation(field, "필수 입력 항목입니다.");
        return text;
    }
    private CommandValidationException validation(String field, String message) { return new CommandValidationException(new FieldError(field, message)); }
    private UUID actorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) {
            AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue());
            if (session != null) return session.userId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
