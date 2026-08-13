package kr.ac.knue.achievement.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface AuthenticationPort {
    LoginResult login(String username, String password);
    AuthenticatedSession findActiveSession(String sessionId);
    boolean logout(String sessionId);

    record AuthenticatedSession(UUID userId, String sessionId, String username, String organizationCode,
            Set<String> roleCodes, Instant expiresAt) { }
    record LoginResult(boolean authenticated, AuthenticatedSession session) {
        public static LoginResult success(AuthenticatedSession session) { return new LoginResult(true, session); }
        public static LoginResult failure() { return new LoginResult(false, null); }
    }
}
