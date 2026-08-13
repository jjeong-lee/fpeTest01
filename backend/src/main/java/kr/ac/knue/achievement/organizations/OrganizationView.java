package kr.ac.knue.achievement.organizations;

public record OrganizationView(String organizationCode, String organizationName, String organizationType,
        String useStatus, String currentParentOrganizationCode) { }
