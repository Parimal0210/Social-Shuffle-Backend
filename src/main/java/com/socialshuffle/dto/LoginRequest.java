package com.socialshuffle.dto;

import java.io.Serializable;

public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String emailOrPhone;
    private String identifier;
    private String email;
    private String phone;
    private String password;
    private String name;
    private String designation;

    public LoginRequest() {
    }

    public LoginRequest(String emailOrPhone, String password) {
        this.emailOrPhone = emailOrPhone;
        this.password = password;
    }

    public String getResolvedIdentifier() {
        if (emailOrPhone != null && !emailOrPhone.trim().isEmpty()) return emailOrPhone.trim();
        if (identifier != null && !identifier.trim().isEmpty()) return identifier.trim();
        if (email != null && !email.trim().isEmpty()) return email.trim();
        if (phone != null && !phone.trim().isEmpty()) return phone.trim();
        return "";
    }

    public String getEmailOrPhone() { return emailOrPhone; }
    public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
