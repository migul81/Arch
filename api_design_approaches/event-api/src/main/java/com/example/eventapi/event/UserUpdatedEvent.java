package com.example.eventapi.event;

import com.example.eventapi.model.User;

public class UserUpdatedEvent extends UserEvent {
    private final User user;
    
    public UserUpdatedEvent(User user) {
        super("USER_UPDATED");
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}
