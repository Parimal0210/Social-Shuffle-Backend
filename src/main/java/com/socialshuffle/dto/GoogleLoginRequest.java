package com.socialshuffle.dto;

import java.io.Serializable;

public class GoogleLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;
    private String name;
    private String avatar;
    private String phone;
    private String area;
    private String googleId;
    private String idToken;
    private String accessToken;
    private String bio;

    public GoogleLoginRequest() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
