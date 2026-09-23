package com.socialshuffle.dto;

public class GoogleLoginRequest {

    private String email;
    private String name;
    private String avatar;
    private String phone;
    private String area;
    private String googleId;

    public GoogleLoginRequest() {
    }

    public GoogleLoginRequest(String email, String name, String avatar, String phone, String area, String googleId) {
        this.email = email;
        this.name = name;
        this.avatar = avatar;
        this.phone = phone;
        this.area = area;
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }
}
