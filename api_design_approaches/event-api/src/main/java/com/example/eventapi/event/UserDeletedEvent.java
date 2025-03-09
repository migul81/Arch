package com.example.eventapi.event;

public class UserDeletedEvent extends UserEvent {
    private final Long userId;
    
    public UserDeletedEvent(Long userId) {
        super("USER_DELETED");
        this.userId = userId;
    }
    
    public Long getUserId() {
        return userId;
    }
}