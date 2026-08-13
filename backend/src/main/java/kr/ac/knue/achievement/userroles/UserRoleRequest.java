package kr.ac.knue.achievement.userroles;

import java.time.LocalDate;
import java.util.UUID;

public record UserRoleRequest(String assignmentType, LocalDate effectiveStartDate, LocalDate effectiveEndDate,
        UUID approverUserId, String reason) { }