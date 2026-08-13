package kr.ac.knue.achievement.menus;

public record MenuPermissionView(String subjectType, String subjectId, String menuId, String topMenuName,
        String middleMenuName, String menuName, String accessDecision) { }
