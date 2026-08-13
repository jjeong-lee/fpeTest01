package kr.ac.knue.achievement.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class LocalAuthenticationAdapter implements AuthenticationPort {
    private final AuthenticationMapper mapper;

    public LocalAuthenticationAdapter(AuthenticationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LoginResult login(String username, String password) {
        AuthenticationMapper.Account account = mapper.findActiveAccount(username, password);
        if (account == null) {
            return LoginResult.failure();
        }
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        mapper.insertSession(sessionId, account.userId(), expiresAt);
        return LoginResult.success(new AuthenticatedSession(account.userId(), sessionId.toString(), account.username(),
                account.organizationCode(), mapper.findActiveRoleCodes(account.userId()), expiresAt));
    }

    @Override
    public AuthenticatedSession findActiveSession(String sessionId) {
        try {
            AuthenticationMapper.SessionAccount session = mapper.findActiveSession(UUID.fromString(sessionId));
            return session == null ? null : new AuthenticatedSession(session.userId(), session.sessionId().toString(),
                    session.username(), session.organizationCode(), mapper.findActiveRoleCodes(session.userId()), session.expiresAt());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public boolean logout(String sessionId) {
        try {
            return mapper.invalidateSession(UUID.fromString(sessionId)) == 1;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
