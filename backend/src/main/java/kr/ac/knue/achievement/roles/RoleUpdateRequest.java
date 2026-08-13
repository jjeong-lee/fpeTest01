package kr.ac.knue.achievement.roles;

public record RoleUpdateRequest(String roleName, String grantCriteria, String dataScopeDefault,
        String useStatus, String reason) { }