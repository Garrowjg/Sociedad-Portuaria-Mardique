package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    long countByUsernameAndIpAddressAndSuccessfulFalseAndAttemptTimeAfter(
        String username, String ipAddress, LocalDateTime attemptTime);
}
