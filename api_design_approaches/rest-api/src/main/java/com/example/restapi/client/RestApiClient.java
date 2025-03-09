package com.example.restapi.client;

import com.example.restapi.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class RestApiClient {
    private static final Logger logger = Logger.getLogger(RestApiClient.class.getName());
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public RestApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() throws IOException, InterruptedException {
        logger.info("Getting all users...");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<User>>() {});
        } else {
            throw new IOException("Failed to get users. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Get a user by ID
     */
    public User getUserById(Long id) throws IOException, InterruptedException {
        logger.info("Getting user with ID: " + id);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + id))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), User.class);
        } else if (response.statusCode() == 404) {
            logger.warning("User not found with ID: " + id);
            return null;
        } else {
            throw new IOException("Failed to get user. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Create a new user
     */
    public User createUser(String name, String email) throws IOException, InterruptedException {
        logger.info("Creating user: " + name + " (" + email + ")");
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        
        String requestBody = objectMapper.writeValueAsString(user);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), User.class);
        } else {
            throw new IOException("Failed to create user. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Update an existing user
     */
    public User updateUser(Long id, String name, String email) throws IOException, InterruptedException {
        logger.info("Updating user with ID: " + id);
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        
        String requestBody = objectMapper.writeValueAsString(user);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), User.class);
        } else if (response.statusCode() == 404) {
            logger.warning("User not found with ID: " + id);
            return null;
        } else {
            throw new IOException("Failed to update user. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Delete a user
     */
    public boolean deleteUser(Long id) throws IOException, InterruptedException {
        logger.info("Deleting user with ID: " + id);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + id))
                .DELETE()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 204) {
            logger.info("User deleted successfully");
            return true;
        } else if (response.statusCode() == 404) {
            logger.warning("User not found with ID: " + id);
            return false;
        } else {
            throw new IOException("Failed to delete user. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Display list of users
     */
    private void displayUsers(List<User> users) {
        logger.info("=== Users ===");
        for (User user : users) {
            logger.info(user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
        }
        logger.info("============");
    }
    
    /**
     * Main method to run the client
     */
    public static void main(String[] args) {
        RestApiClient client = new RestApiClient("http://localhost:8080/api");
        
        try {
            // Get all users to show current state
            List<User> users = client.getAllUsers();
            client.displayUsers(users);
            
            // Create a new user
            User createdUser = client.createUser("Imran Gul", "imrangul@example.com");
            
            logger.info("Created user: " + createdUser.getId() + ": " + createdUser.getName() + " (" + createdUser.getEmail() + ")");
            
            // Get all users to see the new user
            users = client.getAllUsers();
            client.displayUsers(users);
            
            // Get a specific user
            if (users.size() > 0) {
                Long userId = users.get(0).getId();
                User user = client.getUserById(userId);
                if (user != null) {
                    logger.info("Retrieved user: " + user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
                }
            }
            
            // Update a user
            if (users.size() > 1) {
                Long userId = users.get(1).getId();
                User updatedUser = client.updateUser(userId, "Jane Wilson", "jane.wilson@example.com");
                if (updatedUser != null) {
                    logger.info("Updated user: " + updatedUser.getId() + ": " + updatedUser.getName() + " (" + updatedUser.getEmail() + ")");
                }
            }
            
            // Delete a user
            if (users.size() > 2) {
                Long userId = users.get(2).getId();
                boolean deleted = client.deleteUser(userId);
                if (deleted) {
                    logger.info("User deleted successfully with ID: " + userId);
                }
            }
            
            // Get all users to see the results
            users = client.getAllUsers();
            client.displayUsers(users);
            
            // Interactive mode
            Scanner scanner = new Scanner(System.in);
            String command = "";
            
            System.out.println("\nEnter commands (or 'exit' to quit):");
            System.out.println("- list : List all users");
            System.out.println("- get [id] : Get a user by ID");
            System.out.println("- create [name] [email] : Create a new user");
            System.out.println("- update [id] [name] [email] : Update a user");
            System.out.println("- delete [id] : Delete a user");
            
            while (!command.equals("exit")) {
                System.out.print("> ");
                command = scanner.nextLine();
                
                try {
                    if (command.equals("list")) {
                        List<User> allUsers = client.getAllUsers();
                        client.displayUsers(allUsers);
                    } else if (command.startsWith("get ")) {
                        String[] parts = command.split(" ", 2);
                        if (parts.length > 1) {
                            try {
                                Long id = Long.parseLong(parts[1]);
                                User user = client.getUserById(id);
                                if (user != null) {
                                    logger.info("User found: " + user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid ID format");
                            }
                        }
                    } else if (command.startsWith("create ")) {
                        String[] parts = command.split(" ", 3);
                        if (parts.length > 2) {
                            User user = client.createUser(parts[1], parts[2]);
                            logger.info("User created: " + user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
                        } else {
                            System.out.println("Usage: create [name] [email]");
                        }
                    } else if (command.startsWith("update ")) {
                        String[] parts = command.split(" ", 4);
                        if (parts.length > 3) {
                            try {
                                Long id = Long.parseLong(parts[1]);
                                User user = client.updateUser(id, parts[2], parts[3]);
                                if (user != null) {
                                    logger.info("User updated: " + user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid ID format");
                            }
                        } else {
                            System.out.println("Usage: update [id] [name] [email]");
                        }
                    } else if (command.startsWith("delete ")) {
                        String[] parts = command.split(" ", 2);
                        if (parts.length > 1) {
                            try {
                                Long id = Long.parseLong(parts[1]);
                                boolean deleted = client.deleteUser(id);
                                if (deleted) {
                                    logger.info("User deleted successfully with ID: " + id);
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid ID format");
                            }
                        }
                    } else if (!command.equals("exit")) {
                        System.out.println("Unknown command: " + command);
                    }
                } catch (Exception e) {
                    logger.severe("Error executing command: " + e.getMessage());
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
