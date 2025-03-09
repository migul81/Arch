package com.example.eventapi.event;

public class ErrorEvent extends UserEvent {
    private final String message;
    private final int code;
    
    public ErrorEvent(String message, int code) {
        super("ERROR");
        this.message = message;
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getCode() {
        return code;
    }
}