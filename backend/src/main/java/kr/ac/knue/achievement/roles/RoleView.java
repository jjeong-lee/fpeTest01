package kr.ac.knue.achievement.roles;

public record RoleView(String roleCode, String roleName, String purpose, String grantCriteria,
        String dataScopeDefault, String useStatus) { }