package com.example.eventapi.controller;

import com.example.eventapi.dto.UpdateUserRequest;
import com.example.eventapi.dto.UserDTO;
import com.example.eventapi.dto.UserIdRequest;
import com.example.eventapi.event.*;
import com.example.eventapi.model.User;
import com.example.eventapi.repository.UserRepository;
import com.example.eventapi.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class UserController {
    
	private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public UserController(UserService userService, SimpMessagingTemplate messagingTemplate) {
    	this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }
    
    @MessageMapping("/users.getAll")
    @SendTo("/topic/users")
    public UserEvent getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            return new UserListEvent(users);
        } catch (Exception e) {
            return new ErrorEvent("Failed to fetch users: " + e.getMessage(), 500);
        }
    }
    
    @MessageMapping("/users.get")
    @SendTo("/topic/users")
    public UserEvent getUser(UserIdRequest request) {
        try {
            Optional<User> userOpt = userService.findUserById(request.getId());
            if (userOpt.isPresent()) {
                return new GetUserEvent(userOpt.get());
            } else {
                return new ErrorEvent("User not found with id: " + request.getId(), 404);
            }
        } catch (Exception e) {
            return new ErrorEvent("Failed to fetch user: " + e.getMessage(), 500);
        }
    }
    
    @MessageMapping("/users.create")
    @SendTo("/topic/users")
    public UserEvent createUser(UserDTO userDTO) {
        try {
            User user = userDTO.toEntity();
            User savedUser = userService.createUser(user);
            return new UserCreatedEvent(savedUser);
        } catch (Exception e) {
            return new ErrorEvent("Failed to create user: " + e.getMessage(), 500);
        }
    }
    
    @MessageMapping("/users.update")
    @SendTo("/topic/users")
    public UserEvent updateUser(UpdateUserRequest request) {
        try {
            Long userId = request.getId();
            Optional<User> existingUserOpt = userService.findUserById(userId);
            
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                UserDTO userDTO = request.getUser();
                
                existingUser.setName(userDTO.getName());
                existingUser.setEmail(userDTO.getEmail());
                
                User updatedUser = userService.createUser(existingUser);
                return new UserUpdatedEvent(updatedUser);
            } else {
                return new ErrorEvent("User not found with id: " + userId, 404);
            }
        } catch (Exception e) {
            return new ErrorEvent("Failed to update user: " + e.getMessage(), 500);
        }
    }
    
    @MessageMapping("/users.delete")
    @SendTo("/topic/users")
    public UserEvent deleteUser(UserIdRequest request) {
        try {
            Long userId = request.getId();
            Optional<User> existingUserOpt = userService.findUserById(userId);
            
            if (existingUserOpt.isPresent()) {
            	userService.deleteUser(userId);
                return new UserDeletedEvent(userId);
            } else {
                return new ErrorEvent("User not found with id: " + userId, 404);
            }
        } catch (Exception e) {
            return new ErrorEvent("Failed to delete user: " + e.getMessage(), 500);
        }
    }
}
