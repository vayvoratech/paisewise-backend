package com.paisewise.common.service;

import com.paisewise.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public void logAudit(String action, String details) {
        String currentUser = SecurityUtils.getCurrentUserLogin();
        if (currentUser == null) {
            currentUser = "ANONYMOUS_USER";
        }

        // Logs structured audit trails which can be captured by log aggregation tools
        log.info("AUDIT_LOG | User: {} | Action: {} | Details: {}", currentUser, action, details);
    }
}