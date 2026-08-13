package kr.ac.knue.achievement.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationPort authenticationPort;

    public AuthenticationService(AuthenticationPort authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    public AuthenticationPort.LoginResult login(String username, String password) {
        return authenticationPort.login(username, password);
    }

    public AuthenticationPort.AuthenticatedSession findActiveSession(String sessionId) {
        return authenticationPort.findActiveSession(sessionId);
    }

    public boolean logout(String sessionId) {
        return authenticationPort.logout(sessionId);
    }
}
