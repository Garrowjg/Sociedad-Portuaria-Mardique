package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.AuditService;
import com.example.MardiqueWeb.Service.CustomUserDetailsService;
import com.example.MardiqueWeb.Config.CustomAuthenticationFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final AuditService auditService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomAuthenticationFailureHandler failureHandler,
                          AuditService auditService) {
        this.userDetailsService = userDetailsService;
        this.failureHandler = failureHandler;
        this.auditService = auditService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/inicio", "/empresa", "/servicios", "/procedimientos",
                                "/tarifas", "/tramites-en-linea", "/galeria", "/contacto",
                                "/contacto/pqrs", "/contacto/message", "/sesion-activa", "/error",
                                "/css/**", "/js/**", "/images/**", "/videos/**", "/uploads/**",
                                "/login", "/register", "/intranet/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/editor/**").hasRole("EDITOR")
                        .requestMatchers("/user/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard")
                        .successHandler((request, response, authentication) -> {
                            auditService.log(authentication.getName(), "LOGIN_SUCCESS",
                                    "Successful login from IP: " + request.getRemoteAddr(), request);
                            response.sendRedirect("/dashboard");
                        })
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/inicio?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new AntPathRequestMatcher("/contacto/pqrs"),
                        new AntPathRequestMatcher("/contacto/message")))
                .headers(headers -> headers
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://alcdn.msauth.net; " +
                                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com; " +
                                        "img-src 'self' data: https:; " +
                                        "font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com; " +
                                        "frame-src 'self' https://drive.google.com https://maps.google.com https://www.google.com https://login.microsoftonline.com https://helpdesk.agmdesarrollos.com https://app.powerbi.com; " +
                                        "connect-src 'self' https://login.microsoftonline.com https://graph.microsoft.com;"
                        ))
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                );
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}