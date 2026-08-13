package kr.ac.knue.achievement.codes;

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
public class CodeGroupManagementController {
    private static final Set<String> CODE_GROUP_FIELDS = Set.of("groupId", "groupName", "description", "managingDepartment", "reason");

    private final CodeGroupManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public CodeGroupManagementController(CodeGroupManagementService service, AuthenticationPort authenticationPort,
            CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/code-groups")
    public ApiResponse<List<CodeGroupView>> listCodeGroups() {
        return ApiResponse.success(service.listCodeGroups());
    }

    @PostMapping("/api/code-groups")
    public ApiResponse<CodeGroupView> createCodeGroup(@RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        return ApiResponse.success(service.create(command(command), actorId(request)));
    }

    @PutMapping("/api/code-groups/{groupId}")
    public ApiResponse<CodeGroupView> updateCodeGroup(@PathVariable String groupId,
            @RequestBody Map<String, Object> command, HttpServletRequest request) {
        return ApiResponse.success(service.update(groupId, command(command), actorId(request)));
    }

    private CodeGroupRequest command(Map<String, Object> command) {
        commandValidator.rejectUnexpectedFields(command, CODE_GROUP_FIELDS);
        return new CodeGroupRequest(requiredString(command, "groupId"), requiredString(command, "groupName"),
                optionalString(command, "description"), optionalString(command, "managingDepartment"),
                requiredString(command, "reason"));
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
