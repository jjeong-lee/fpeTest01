package kr.ac.knue.achievement.users;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserView(
        UUID userId,
        String employeeNo,
        String name,
        String organizationCode,
        String position,
        String employmentStatus,
        String duty,
        LocalDate retirementDate,
        LocalDateTime lastSyncedAt,
        boolean systemEnabled,
        List<String> roleCodes) {
}
