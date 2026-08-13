package kr.ac.knue.achievement.codes;

import java.util.LinkedHashMap;
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
public class DetailCodeManagementController {
    private static final Set<String> DETAIL_CODE_FIELDS = Set.of("groupId", "codeValue", "codeName", "parentDetailCodeId", "displayOrder", "additionalAttributes", "reason");

    private final DetailCodeManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public DetailCodeManagementController(DetailCodeManagementService service, AuthenticationPort authenticationPort,
            CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/code-groups/{groupId}/detail-codes")
    public ApiResponse<?> listDetailCodes(@PathVariable String groupId) {
        return ApiResponse.success(service.listByGroup(groupId));
    }

    @PostMapping("/api/detail-codes")
    public ApiResponse<DetailCodeView> createDetailCode(@RequestBody Map<String, Object> command, HttpServletRequest request) {
        return ApiResponse.success(service.create(command(command), actorId(request)));
    }

    @PutMapping("/api/detail-codes/{detailCodeId}")
    public ApiResponse<DetailCodeView> updateDetailCode(@PathVariable UUID detailCodeId, @RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        return ApiResponse.success(service.update(detailCodeId, command(command), actorId(request)));
    }

    private DetailCodeRequest command(Map<String, Object> command) {
        commandValidator.rejectUnexpectedFields(command, DETAIL_CODE_FIELDS);
        return new DetailCodeRequest(requiredString(command, "groupId"), requiredString(command, "codeValue"),
                requiredString(command, "codeName"), optionalUuid(command, "parentDetailCodeId"), requiredNonNegativeInt(command, "displayOrder"),
                optionalAttributes(command), requiredString(command, "reason"));
    }

    private String requiredString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw validation(field, "필수 입력 항목입니다.");
        return text.trim();
    }

    private UUID optionalUuid(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (value == null || "".equals(value)) return null;
        if (!(value instanceof String text)) throw validation(field, "UUID 형식이어야 합니다.");
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException exception) {
            throw validation(field, "UUID 형식이어야 합니다.");
        }
    }

    private int requiredNonNegativeInt(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof Number number) || number.intValue() < 0 || number.doubleValue() != number.intValue()) {
            throw validation(field, "0 이상의 정수여야 합니다.");
        }
        return number.intValue();
    }

    private Map<String, Object> optionalAttributes(Map<String, Object> command) {
        Object value = command.get("additionalAttributes");
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> source)) throw validation("additionalAttributes", "객체 형식이어야 합니다.");
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw validation("additionalAttributes", "객체 키는 문자열이어야 합니다.");
            attributes.put(key, entry.getValue());
        }
        return Map.copyOf(attributes);
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
