package com.socialshuffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "admins", indexes = {
    @Index(name = "idx_admin_email", columnList = "email")
})
public class AdminAccount {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String role = "admin";

    @Column(length = 100)
    private String designation = "Host & Community Organizer";

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private boolean active = true;

    @Column(length = 50)
    private String createdAt;

    public AdminAccount() {
    }

    public AdminAccount(String id, String name, String email, String phone, String password, String designation, String avatar) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.designation = designation;
        this.avatar = avatar;
        this.createdAt = "2024-01-01T00:00:00Z";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
