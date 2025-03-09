package com.example.eventapi.dto;

public class UserIdRequest {
    private Long id;
    
    public UserIdRequest() {}
    
    public UserIdRequest(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
