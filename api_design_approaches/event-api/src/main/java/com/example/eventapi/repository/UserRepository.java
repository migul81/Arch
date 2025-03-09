package com.example.eventapi.repository;

import com.example.eventapi.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // Row mapper for User
    private static final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        return user;
    };
    
    @PostConstruct
    public void initialize() {
        // Create users table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255) NOT NULL)");
        
        // Insert initial data if the table is empty
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", "John Doe", "john@example.com");
            jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", "Jane Smith", "jane@example.com");
            jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", "Bob Johnson", "bob@example.com");
        }
    }
    
    public List<User> findAll() {
        return jdbcTemplate.query("SELECT id, name, email FROM users", userRowMapper);
    }
    
    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query("SELECT id, name, email FROM users WHERE id = ?", userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            // Insert new user
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO users (name, email) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                return ps;
            }, keyHolder);
            user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
            return user;
        } else {
            // Update existing user
            jdbcTemplate.update(
                    "UPDATE users SET name = ?, email = ? WHERE id = ?",
                    user.getName(), user.getEmail(), user.getId());
            return user;
        }
    }
    
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }
}