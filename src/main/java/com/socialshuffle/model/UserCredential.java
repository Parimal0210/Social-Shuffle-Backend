package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_credentials", indexes = {
    @Index(name = "idx_creds_email", columnList = "email", unique = true),
    @Index(name = "idx_creds_phone", columnList = "phone"),
    @Index(name = "idx_creds_subject", columnList = "subject_type, subject_id")
})
public class UserCredential implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType; // "admin" or "participant"

    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone = "";

    @Column(name = "username", length = 150)
    private String username = "";

    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "auth_provider", length = 30)
    private String authProvider = "local";

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Long lockedUntil;

    @Column(name = "last_login_at", length = 50)
    private String lastLoginAt;

    @Column(name = "password_updated_at", length = 50)
    private String passwordUpdatedAt;

    @Column(name = "created_at", nullable = false, length = 50)
    private String createdAt;

    @Column(name = "updated_at", nullable = false, length = 50)
    private String updatedAt;

    public UserCredential() {
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public Long getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Long lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getPasswordUpdatedAt() { return passwordUpdatedAt; }
    public void setPasswordUpdatedAt(String passwordUpdatedAt) { this.passwordUpdatedAt = passwordUpdatedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
