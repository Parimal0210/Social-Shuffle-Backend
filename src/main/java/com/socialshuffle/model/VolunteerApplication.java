package com.socialshuffle.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "volunteers")
public class VolunteerApplication {

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String area;
    private String reason;
    private String experience;
    private String skills;
    private String availability;
    private List<String> preferredResponsibilities = new ArrayList<>();
    private String additionalInfo;
    private String status = "New"; // New, Under Review, Accepted, Rejected, On Hold
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
