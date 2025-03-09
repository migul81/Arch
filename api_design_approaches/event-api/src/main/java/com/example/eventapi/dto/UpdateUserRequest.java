package com.example.eventapi.dto;

public class UpdateUserRequest {
    private Long id;
    private UserDTO user;
    
    public UpdateUserRequest() {}
    
    public UpdateUserRequest(Long id, UserDTO user) {
        this.id = id;
        this.user = user;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UserDTO getUser() {
        return user;
    }
    
    public void setUser(UserDTO user) {
        this.user = user;
    }
}