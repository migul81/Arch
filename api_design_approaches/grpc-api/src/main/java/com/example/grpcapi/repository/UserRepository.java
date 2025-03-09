package com.example.grpcapi.repository;

import com.example.grpcapi.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1", "sa", "");
    }
    
    public UserRepository() {
        try {
            // Initialize the database and create the users table
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            
            // Create the users table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255) NOT NULL)"
            );
            
            // Insert initial data if the table is empty
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                statement.execute(
                    "INSERT INTO users (name, email) VALUES " +
                    "('John Doe', 'john@example.com'), " +
                    "('Jane Smith', 'jane@example.com'), " +
                    "('Bob Johnson', 'bob@example.com')"
                );
            }
            
            rs.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, name, email FROM users")) {
            
            while (rs.next()) {
                User user = new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
        
        return users;
    }
    
    public Optional<User> findById(Long id) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, name, email FROM users WHERE id = ?")) {
            
            statement.setLong(1, id);
            ResultSet rs = statement.executeQuery();
            
            if (rs.next()) {
                User user = new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email")
                );
                rs.close();
                return Optional.of(user);
            } else {
                rs.close();
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user with id: " + id, e);
        }
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            // Insert new user
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (name, email) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.executeUpdate();
                
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
                
                generatedKeys.close();
                return user;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create user", e);
            }
        } else {
            // Update existing user
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET name = ?, email = ? WHERE id = ?")) {
                
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setLong(3, user.getId());
                statement.executeUpdate();
                
                return user;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update user with id: " + user.getId(), e);
            }
        }
    }
    
    public void deleteById(Long id) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM users WHERE id = ?")) {
            
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user with id: " + id, e);
        }
    }
}