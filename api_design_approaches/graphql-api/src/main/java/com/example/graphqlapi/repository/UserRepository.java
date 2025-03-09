package com.example.graphqlapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.graphqlapi.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}

