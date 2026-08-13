package kr.ac.knue.achievement.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CommandValidationApiTest {
    private final CommandValidator validator = new CommandValidator();

    @Test
    void rejectsMissingReasonBeforeACommandCanMutatePersistence() {
        assertThrows(CommandValidationException.class,
                () -> validator.requireReason(Map.of("systemEnabled", true), "reason"));
    }

    @Test
    void rejectsFieldsOutsideTheAllowedChangeSetBeforeACommandCanMutatePersistence() {
        assertThrows(CommandValidationException.class,
                () -> validator.rejectUnexpectedFields(Map.of("systemEnabled", true, "name", "변경 금지"), Set.of("systemEnabled", "reason")));
    }
}
