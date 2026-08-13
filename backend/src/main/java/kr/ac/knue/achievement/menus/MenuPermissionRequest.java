package kr.ac.knue.achievement.menus;

public record MenuPermissionRequest(String subjectType, String subjectId, String menuId, String accessDecision,
        String reason) { }
