package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_expires", columnList = "expires_at")
})
public class OtpVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "otp", nullable = false, length = 20)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Long verifiedAt;

    @Column(name = "name", length = 255)
    private String name;

    public OtpVerification() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Long getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Long verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
