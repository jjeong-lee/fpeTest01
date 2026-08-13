package kr.ac.knue.achievement.users;

import java.util.List;

public record UserSystemSettingsRequest(Boolean systemEnabled, List<String> roleCodes, String reason) {
}
