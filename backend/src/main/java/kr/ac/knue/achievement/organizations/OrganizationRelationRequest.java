package kr.ac.knue.achievement.organizations;

import java.time.LocalDate;

public record OrganizationRelationRequest(String parentOrganizationCode, LocalDate effectiveStartDate,
        LocalDate effectiveEndDate, String reason) { }
