package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.LoginAttempt;
import com.example.MardiqueWeb.Repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_MINUTES = 15;

    public void loginFailed(String username, String ip) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ip);
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setSuccessful(false);
        loginAttemptRepository.save(attempt);
    }

    public boolean isBlocked(String username, String ip) {
        LocalDateTime blockTime = LocalDateTime.now().minusMinutes(BLOCK_MINUTES);
        long failedAttempts = loginAttemptRepository
            .countByUsernameAndIpAddressAndSuccessfulFalseAndAttemptTimeAfter(username, ip, blockTime);
        return failedAttempts >= MAX_ATTEMPTS;
    }

    public int getAttempts(String username, String ip) {
        LocalDateTime blockTime = LocalDateTime.now().minusMinutes(BLOCK_MINUTES);
        return (int) loginAttemptRepository
            .countByUsernameAndIpAddressAndSuccessfulFalseAndAttemptTimeAfter(username, ip, blockTime);
    }

    public void loginSucceeded(String username, String ip) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ip);
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setSuccessful(true);
        loginAttemptRepository.save(attempt);
    }
}
