package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.AuditService;
import com.example.MardiqueWeb.Service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuditService auditService;

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_MS = 15 * 60 * 1000;

    public static final ConcurrentMap<String, Long> blockedUsers = new ConcurrentHashMap<>();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");

        try {
            auditService.log(username, "LOGIN_FAILED", "Failed login attempt from IP: " + request.getRemoteAddr(), request);
        } catch (Exception ignored) {}

        try {
            loginAttemptService.loginFailed(username, request.getRemoteAddr());
        } catch (Exception ignored) {}

        int attempts = getSessionAttempts(request, username);

        if (attempts >= MAX_ATTEMPTS) {
            blockedUsers.put(username, System.currentTimeMillis() + BLOCK_MS);
            setDefaultFailureUrl("/login?blocked");
        } else if (attempts >= 3) {
            setDefaultFailureUrl("/login?error&attempts=" + attempts);
        } else {
            setDefaultFailureUrl("/login?error");
        }
        super.onAuthenticationFailure(request, response, exception);
    }

    public static boolean isBlocked(String username) {
        Long expiry = blockedUsers.get(username);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blockedUsers.remove(username);
            return false;
        }
        return true;
    }

    public static void clearBlock(String username) {
        blockedUsers.remove(username);
    }

    public static int getSessionAttempts(HttpServletRequest request, String username) {
        String key = "login_attempts_" + (username != null ? username : "UNKNOWN");
        Integer count = (Integer) request.getSession().getAttribute(key);
        if (count == null) count = 0;
        count++;
        request.getSession().setAttribute(key, count);
        return count;
    }

    public static void clearSessionAttempts(HttpServletRequest request, String username) {
        if (username == null) return;
        request.getSession().removeAttribute("login_attempts_" + username);
    }
}
