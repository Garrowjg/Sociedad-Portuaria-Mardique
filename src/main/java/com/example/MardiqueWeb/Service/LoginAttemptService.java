package com.example.MardiqueWeb.Service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedUntil = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION = 15 * 60 * 1000;

    public void loginFailed(String username, String ip) {
        String key = username + "|" + ip;
        attempts.put(key, attempts.getOrDefault(key, 0) + 1);
    }

    public boolean isBlocked(String username, String ip) {
        String key = username + "|" + ip;
        if (blockedUntil.containsKey(key)) {
            if (System.currentTimeMillis() < blockedUntil.get(key)) {
                return true;
            }
            blockedUntil.remove(key);
            attempts.remove(key);
        }
        if (attempts.getOrDefault(key, 0) >= MAX_ATTEMPTS) {
            blockedUntil.put(key, System.currentTimeMillis() + BLOCK_DURATION);
            return true;
        }
        return false;
    }

    public int getAttempts(String username, String ip) {
        String key = username + "|" + ip;
        return attempts.getOrDefault(key, 0);
    }

    public void loginSucceeded(String username, String ip) {
        String key = username + "|" + ip;
        attempts.remove(key);
        blockedUntil.remove(key);
    }
}
