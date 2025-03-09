package com.example.eventapi.event;

import com.example.eventapi.model.User;

public class GetUserEvent extends UserEvent {
    private final User user;
    
    public GetUserEvent(User user) {
        super("GET_USER");
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}