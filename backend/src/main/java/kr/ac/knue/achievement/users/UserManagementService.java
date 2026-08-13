package kr.ac.knue.achievement.users;

import java.util.UUID;

public interface UserManagementService {
    UserSearchResult search(UserSearchCriteria criteria);
    UserView updateSystemSettings(UUID userId, UserSystemSettingsRequest request, UUID actorUserId);
}
