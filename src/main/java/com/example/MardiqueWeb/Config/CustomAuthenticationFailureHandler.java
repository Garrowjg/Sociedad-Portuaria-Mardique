package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();

        loginAttemptService.loginFailed(username, ip);

        if (loginAttemptService.isBlocked(username, ip)) {
            setDefaultFailureUrl("/login?blocked");
        } else {
            int n = loginAttemptService.getAttempts(username, ip);
            if (n > 3) {
                setDefaultFailureUrl("/login?error&attempts=" + n);
            } else {
                setDefaultFailureUrl("/login?error");
            }
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
