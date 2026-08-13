package kr.ac.knue.achievement.menus;

import java.util.UUID;

public record MenuParentRequest(UUID parentMenuId, String reason) { }
