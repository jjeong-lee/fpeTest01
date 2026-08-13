package kr.ac.knue.achievement.organizations;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.common.CommandValidationException;
import kr.ac.knue.achievement.common.CommandValidator;
import kr.ac.knue.achievement.common.ApiError.FieldError;

@RestController
public class OrganizationManagementController {
    private final OrganizationManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public OrganizationManagementController(OrganizationManagementService service, AuthenticationPort authenticationPort,
            CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/organizations")
    public ApiResponse<OrganizationManagementService.OrganizationSearchResult> search(
            @RequestParam(required = false) String organizationCode, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "페이지 정보를 확인해 주세요.");
        return ApiResponse.success(service.search(organizationCode, page, Math.min(size, 100)));
    }

    @GetMapping("/api/organizations/tree")
    public ApiResponse<OrganizationTreeView> tree(@RequestParam String organizationCode) {
        return ApiResponse.success(service.tree(organizationCode));
    }

    @PutMapping("/api/organization-relations/{organizationId}")
    public ApiResponse<OrganizationTreeView> saveRelation(@PathVariable String organizationId,
            @RequestBody Map<String, Object> command, HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command,
                Set.of("parentOrganizationCode", "effectiveStartDate", "effectiveEndDate", "reason"));
        commandValidator.requireReason(command, "reason");
        LocalDate effectiveStartDate = parseDate(command.get("effectiveStartDate"), "effectiveStartDate", true);
        LocalDate effectiveEndDate = parseDate(command.get("effectiveEndDate"), "effectiveEndDate", false);
        String parentOrganizationCode = optionalString(command.get("parentOrganizationCode"), "parentOrganizationCode");
        return ApiResponse.success(service.saveRelation(organizationId,
                new OrganizationRelationRequest(parentOrganizationCode, effectiveStartDate, effectiveEndDate,
                        (String) command.get("reason")), actorId(request)));
    }

    private LocalDate parseDate(Object value, String field, boolean required) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            if (required) throw new CommandValidationException(new FieldError(field, "적용 시작일은 필수입니다."));
            return null;
        }
        if (!(value instanceof String text)) throw new CommandValidationException(new FieldError(field, "날짜 형식이 올바르지 않습니다."));
        try {
            return LocalDate.parse(text);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new CommandValidationException(new FieldError(field, "날짜 형식이 올바르지 않습니다."));
        }
    }

    private String optionalString(Object value, String field) {
        if (value == null) return null;
        if (!(value instanceof String text)) throw new CommandValidationException(new FieldError(field, "문자열 형식이 올바르지 않습니다."));
        return text.isBlank() ? null : text;
    }

    private UUID actorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) {
            AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue());
            if (session != null) return session.userId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
