package kr.ac.knue.achievement.menus;

import java.util.UUID;

public record MenuInformationView(UUID menuId, String menuName, String screenId, String url,
        String icon, String businessCategory, String description, String useStatus) { }
