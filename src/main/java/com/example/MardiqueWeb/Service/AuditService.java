package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.AuditLog;
import com.example.MardiqueWeb.Repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String username, String action, String details, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(java.time.LocalDateTime.now());
        if (request != null) {
            log.setIpAddress(request.getRemoteAddr());
        }
        auditLogRepository.save(log);
    }

    public void log(String username, String action, String details) {
        log(username, action, details, null);
    }
}
