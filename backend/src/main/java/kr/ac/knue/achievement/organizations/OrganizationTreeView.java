package kr.ac.knue.achievement.organizations;

import java.util.List;

public record OrganizationTreeView(String organizationCode, String organizationName, String organizationType,
        String useStatus, OrganizationView parent, List<OrganizationView> children,
        List<OrganizationRelationHistoryView> relationHistory) { }
