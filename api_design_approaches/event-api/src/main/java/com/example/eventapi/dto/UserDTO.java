package com.example.eventapi.dto;

import com.example.eventapi.model.User;

public class UserDTO {
    private String name;
    private String email;
    
    // Default constructor
    public UserDTO() {}
    
    // Constructor with fields
    public UserDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public User toEntity() {
        User user = new User();
        user.setName(this.name);
        user.setEmail(this.email);
        return user;
    }
}
