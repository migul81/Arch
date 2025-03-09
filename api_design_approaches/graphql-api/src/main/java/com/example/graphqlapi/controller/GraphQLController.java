package com.example.graphqlapi.controller;

import com.example.graphqlapi.model.User;
import com.example.graphqlapi.model.UserInput;
import com.example.graphqlapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphQLController {
    
    private final UserService userService;
    
    @Autowired
    public GraphQLController(UserService userService) {
        this.userService = userService;
    }
    
    @QueryMapping
    public List<User> users() {
        return userService.findAll();
    }
    
    @QueryMapping
    public User userById(@Argument Long id) {
        return userService.findById(id);
    }
    
    @MutationMapping
    public User createUser(@Argument UserInput input) {
        User user = new User();
        user.setName(input.getName());
        user.setEmail(input.getEmail());
        return userService.create(user);
    }
    
    @MutationMapping
    public User updateUser(@Argument Long id, @Argument UserInput input) {
        User userToUpdate = new User();
        userToUpdate.setName(input.getName());
        userToUpdate.setEmail(input.getEmail());
        return userService.update(id, userToUpdate);
    }
    
    @MutationMapping
    public Boolean deleteUser(@Argument Long id) {
        userService.delete(id);
        return true;
    }
}
