package kr.ac.knue.achievement.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.menus.MenuAccessPort;

@RestController
public class AuthenticationController {
    private static final String SESSION_COOKIE = "SESSION";
    private final AuthenticationService authenticationService;
    private final MenuAccessPort menuAccessPort;

    public AuthenticationController(AuthenticationService authenticationService, MenuAccessPort menuAccessPort) {
        this.authenticationService = authenticationService;
        this.menuAccessPort = menuAccessPort;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationPort.LoginResult result = authenticationService.login(request.username(), request.password());
        if (!result.authenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "사용자명 또는 비밀번호가 올바르지 않습니다.");
        }
        response.addHeader("Set-Cookie", sessionCookie(result.session().sessionId(), false).toString());
        return ApiResponse.success(currentUser(result.session()));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = sessionId(request);
        if (sessionId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
        }
        if (!authenticationService.logout(sessionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "활성 세션이 아닙니다.");
        }
        response.addHeader("Set-Cookie", sessionCookie("", true).toString());
        return ApiResponse.success(null);
    }

    @GetMapping("/api/auth/me")
    public ApiResponse<CurrentUserResponse> me(HttpServletRequest request) {
        return ApiResponse.success(currentUser(requireSession(request)));
    }

    private AuthenticationPort.AuthenticatedSession requireSession(HttpServletRequest request) {
        String sessionId = sessionId(request);
        AuthenticationPort.AuthenticatedSession session = sessionId == null ? null : authenticationService.findActiveSession(sessionId);
        if (session == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
        }
        return session;
    }

    private CurrentUserResponse currentUser(AuthenticationPort.AuthenticatedSession session) {
        return new CurrentUserResponse(session.username(), session.roleCodes().stream().sorted().toList(),
                menuAccessPort.allowedMenuPaths(session.userId(), session.organizationCode(), session.roleCodes()));
    }

    private String sessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (SESSION_COOKIE.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private ResponseCookie sessionCookie(String value, boolean expired) {
        return ResponseCookie.from(SESSION_COOKIE, value).httpOnly(true).sameSite("Lax").secure(false)
                .path("/").maxAge(expired ? 0 : 3600).build();
    }
}
