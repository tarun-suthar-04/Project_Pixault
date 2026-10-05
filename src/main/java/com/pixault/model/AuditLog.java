package com.pixault.model;

import java.time.LocalDateTime;

/**
 * Immutable audit log entry.
 */
public class AuditLog {

    private long id;
    private Integer userId;       // nullable for unauthenticated events
    private String eventType;
    private String ipAddress;
    private String deviceInfo;
    private String details;
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(Integer userId, String eventType, String ipAddress,
                    String deviceInfo, String details) {
        this.userId = userId;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // ===== Common Event Types =====
    public static final String LOGIN_SUCCESS      = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE      = "LOGIN_FAILURE";
    public static final String ACCOUNT_LOCKED     = "ACCOUNT_LOCKED";
    public static final String REGISTER           = "REGISTER";
    public static final String EMAIL_VERIFIED     = "EMAIL_VERIFIED";
    public static final String PASSWORD_RESET     = "PASSWORD_RESET";
    public static final String PASSWORD_CHANGED   = "PASSWORD_CHANGED";
    public static final String HIDE_DATA          = "HIDE_DATA";
    public static final String EXTRACT_DATA       = "EXTRACT_DATA";
    public static final String SHARE_CREATED      = "SHARE_CREATED";
    public static final String SHARE_ACCESSED     = "SHARE_ACCESSED";
    public static final String SHARE_EXPIRED      = "SHARE_EXPIRED";
    public static final String SESSION_INVALIDATED = "SESSION_INVALIDATED";
    public static final String TAMPER_DETECTED    = "TAMPER_DETECTED";

    // ===== Getters & Setters =====

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "AuditLog{id=" + id + ", userId=" + userId + ", eventType='" + eventType
                + "', timestamp=" + timestamp + "}";
    }
}
