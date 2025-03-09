package com.example.restapi.service;


import com.example.restapi.model.User;
import com.example.restapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
 
 private final UserRepository userRepository;
 
 @Autowired
 public UserService(UserRepository userRepository) {
     this.userRepository = userRepository;
 }
 
 public List<User> findAll() {
     return userRepository.findAll();
 }
 
 public User findById(Long id) {
     return userRepository.findById(id)
         .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
 }
 
 public User create(User user) {
     return userRepository.save(user);
 }
 
 public User update(Long id, User userDetails) {
     User user = findById(id);
     user.setName(userDetails.getName());
     user.setEmail(userDetails.getEmail());
     return userRepository.save(user);
 }
 
 public void delete(Long id) {
     userRepository.deleteById(id);
 }
}

