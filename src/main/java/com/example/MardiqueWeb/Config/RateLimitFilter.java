package com.example.MardiqueWeb.Config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        int limit = path.startsWith("/login") ? MAX_LOGIN_ATTEMPTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        RequestWindow window = requestCounts.compute(ip, (key, w) -> {
            if (w == null || System.currentTimeMillis() - w.start > WINDOW_MS) {
                return new RequestWindow(System.currentTimeMillis());
            }
            return w;
        });

        if (window.counter.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiadas peticiones. Intenta de nuevo en un minuto.");
            return;
        }

        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestWindow {
        final long start;
        final AtomicInteger counter = new AtomicInteger(0);

        RequestWindow(long start) {
            this.start = start;
        }
    }
}
