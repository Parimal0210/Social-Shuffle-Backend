package com.socialshuffle.dto;

import com.socialshuffle.model.Participant;
import com.socialshuffle.model.User;
import java.io.Serializable;

public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private User user;
    private Participant participant;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, User user, Participant participant, String message) {
        this.token = token;
        this.user = user;
        this.participant = participant;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
