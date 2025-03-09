package com.example.eventapi.event;

import com.example.eventapi.model.User;

public class UserCreatedEvent extends UserEvent {
    private final User user;
    
    public UserCreatedEvent(User user) {
        super("USER_CREATED");
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}