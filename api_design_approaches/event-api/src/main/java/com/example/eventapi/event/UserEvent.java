package com.example.eventapi.event;

public abstract class UserEvent {
    private final String type;
    
    public UserEvent(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
}
