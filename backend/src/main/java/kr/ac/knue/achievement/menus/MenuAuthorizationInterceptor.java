package kr.ac.knue.achievement.menus;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiException;

@Component
public class MenuAuthorizationInterceptor implements HandlerInterceptor {
    private final AuthenticationPort authenticationPort;
    private final MenuAccessPort menuAccessPort;

    public MenuAuthorizationInterceptor(AuthenticationPort authenticationPort, MenuAccessPort menuAccessPort) {
        this.authenticationPort = authenticationPort;
        this.menuAccessPort = menuAccessPort;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String menuPath = menuPathFor(request.getRequestURI());
        if (menuPath == null) {
            return true;
        }
        AuthenticationPort.AuthenticatedSession session = findSession(request);
        if (session == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
        }
        if (!menuAccessPort.mayAccess(session.userId(), session.organizationCode(), session.roleCodes(), menuPath)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MENU_ACCESS_DENIED", "메뉴 접근 권한이 없습니다.");
        }
        return true;
    }

    private AuthenticationPort.AuthenticatedSession findSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("SESSION".equals(cookie.getName())) {
                return authenticationPort.findActiveSession(cookie.getValue());
            }
        }
        return null;
    }

    private String menuPathFor(String requestUri) {
        if (requestUri.startsWith("/api/users/") && requestUri.contains("/roles")) return "/system/user-roles";
        if (requestUri.startsWith("/api/users")) return "/system/users";
        if (requestUri.startsWith("/api/organizations") || requestUri.startsWith("/api/organization-relations")) return "/system/organizations";
        if (requestUri.startsWith("/api/roles")) return "/system/roles";
        if (requestUri.startsWith("/api/menu-permissions")) return "/system/menu-permissions";
        if (requestUri.startsWith("/api/menu-structure")) return "/system/menu-structure";
        if (requestUri.matches("/api/menus/[^/]+/(parent|display-order)")) return "/system/menu-structure";
        if (requestUri.startsWith("/api/menus")) return "/system/menu-information";
        if (requestUri.matches("/api/code-groups/[^/]+/detail-codes")) return "/system/detail-codes";
        if (requestUri.startsWith("/api/code-groups")) return "/system/code-groups";
        if (requestUri.startsWith("/api/detail-codes")) return "/system/detail-codes";
        return null;
    }
}
