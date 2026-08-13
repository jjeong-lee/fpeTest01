package kr.ac.knue.achievement.menus;

public record MenuInformationUpdateRequest(String menuName, String screenId, String url,
        String icon, String businessCategory, String description, String reason) { }
