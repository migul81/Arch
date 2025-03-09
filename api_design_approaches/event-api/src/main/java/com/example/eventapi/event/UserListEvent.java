package com.example.eventapi.event;

import com.example.eventapi.model.User;
import java.util.List;

public class UserListEvent extends UserEvent {
    private final List<User> users;
    
    public UserListEvent(List<User> users) {
        super("USER_LIST");
        this.users = users;
    }
    
    public List<User> getUsers() {
        return users;
    }
}