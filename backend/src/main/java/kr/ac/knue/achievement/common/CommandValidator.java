package kr.ac.knue.achievement.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class CommandValidator {
    public void requireReason(Map<String, ?> command, String field) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new CommandValidationException(new ApiError.FieldError(field, "변경 사유는 필수입니다."));
        }
    }

    public void rejectUnexpectedFields(Map<String, ?> command, Set<String> allowedFields) {
        ArrayList<ApiError.FieldError> errors = new ArrayList<>();
        command.keySet().stream()
                .filter(field -> !allowedFields.contains(field))
                .forEach(field -> errors.add(new ApiError.FieldError(field, "변경할 수 없는 항목입니다.")));
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }
    }
}
