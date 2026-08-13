package kr.ac.knue.achievement.users;

import java.util.List;

public record UserSearchResult(List<UserView> content, long totalElements, int page, int size) {
}
