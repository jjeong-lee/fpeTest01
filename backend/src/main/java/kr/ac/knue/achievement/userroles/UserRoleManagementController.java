package kr.ac.knue.achievement.userroles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
public class UserRoleManagementController {
    private final UserRoleManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public UserRoleManagementController(UserRoleManagementService service, AuthenticationPort authenticationPort, CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/users/{userId}/roles")
    public ApiResponse<RoleListResponse> list(@PathVariable UUID userId) { return ApiResponse.success(new RoleListResponse(service.listCurrentRoles(userId))); }

    @PutMapping("/api/users/{userId}/roles/{roleCode}")
    public ApiResponse<UserRoleView> grant(@PathVariable UUID userId, @PathVariable String roleCode, @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("assignmentType", "effectiveStartDate", "effectiveEndDate", "approverUserId", "reason"));
        commandValidator.requireReason(command, "reason");
        UUID approver = uuid(command, "approverUserId", true);
        return ApiResponse.success(service.grantOrUpdate(userId, roleCode, new UserRoleRequest(text(command, "assignmentType"), date(command, "effectiveStartDate"), date(command, "effectiveEndDate"), approver, text(command, "reason")), actorId(request)));
    }

    @PostMapping("/api/users/{userId}/roles/{roleCode}/revocation")
    public ApiResponse<UserRoleView> revoke(@PathVariable UUID userId, @PathVariable String roleCode, @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("reason"));
        commandValidator.requireReason(command, "reason");
        return ApiResponse.success(service.revoke(userId, roleCode, new UserRoleRevocationRequest(text(command, "reason")), actorId(request)));
    }


    private String text(Map<String, Object> command, String field) { Object value = command.get(field); if (value == null) return null; if (!(value instanceof String text)) throw invalid(field); return text; }
    private UUID uuid(Map<String, Object> command, String field, boolean required) { String value = text(command, field); if (value == null && required) throw new CommandValidationException(new FieldError(field, "승인자는 필수입니다.")); try { return value == null ? null : UUID.fromString(value); } catch (IllegalArgumentException exception) { throw invalid(field); } }
    private LocalDate date(Map<String, Object> command, String field) { String value = text(command, field); try { return value == null ? null : LocalDate.parse(value); } catch (java.time.format.DateTimeParseException exception) { throw invalid(field); } }
    private CommandValidationException invalid(String field) { return new CommandValidationException(new FieldError(field, "형식이 올바르지 않습니다.")); }
    private UUID actorId(HttpServletRequest request) { Cookie[] cookies = request.getCookies(); if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) { AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue()); if (session != null) return session.userId(); } throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다."); }

    public record RoleListResponse(List<UserRoleView> content) { }
}