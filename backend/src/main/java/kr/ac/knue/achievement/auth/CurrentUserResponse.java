package kr.ac.knue.achievement.auth;

import java.util.List;

public record CurrentUserResponse(String username, List<String> roles, List<String> allowedMenuPaths) { }
