package com.socialshuffle.model;

import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "volunteer_applications", indexes = {
    @Index(name = "idx_vol_status", columnList = "status"),
    @Index(name = "idx_vol_email", columnList = "email")
})
public class VolunteerApplication {

    @Id
    @Column(length = 64)
    private String id;

    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    private String area;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String availability;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> preferredResponsibilities = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(length = 30)
    private String status = "New"; // New, Under Review, Accepted, Rejected, On Hold

    @Column(length = 50)
    private String submittedAt;

    public VolunteerApplication() {
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

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public List<String> getPreferredResponsibilities() {
        return preferredResponsibilities;
    }

    public void setPreferredResponsibilities(List<String> preferredResponsibilities) {
        this.preferredResponsibilities = preferredResponsibilities != null ? preferredResponsibilities : new ArrayList<>();
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }
}
