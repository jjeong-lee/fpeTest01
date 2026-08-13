package kr.ac.knue.achievement.organizations;

import java.time.LocalDate;

public record OrganizationRelationHistoryView(String organizationCode, String parentOrganizationCode,
        LocalDate effectiveStartDate, LocalDate effectiveEndDate, String status) { }
