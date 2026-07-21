package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.AuditService;
import com.example.MardiqueWeb.Service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class LoginAttemptAwareAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuditService auditService;

    @Autowired
    public LoginAttemptAwareAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();

        if (CustomAuthenticationFailureHandler.isBlocked(username)) {
            throw new LockedException("Demasiados intentos. Cuenta bloqueada por 15 minutos.");
        }

        try {
            Authentication result = super.authenticate(authentication);
            CustomAuthenticationFailureHandler.clearBlock(username);
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                CustomAuthenticationFailureHandler.clearSessionAttempts(attrs.getRequest(), username);
                try {
                    loginAttemptService.loginSucceeded(username, attrs.getRequest().getRemoteAddr());
                    auditService.log(username, "LOGIN_SUCCESS", "Login successful", attrs.getRequest());
                } catch (Exception ignored) {}
            }
            return result;
        } catch (BadCredentialsException e) {
            throw e;
        }
    }
}
