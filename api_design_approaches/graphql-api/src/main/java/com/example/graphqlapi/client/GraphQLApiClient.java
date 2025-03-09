package com.example.graphqlapi.client;

import com.example.graphqlapi.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class GraphQLApiClient {
    private static final Logger logger = Logger.getLogger(GraphQLApiClient.class.getName());
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public GraphQLApiClient(String endpoint) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Execute a GraphQL query
     */
    private JsonNode executeGraphQL(String query, ObjectNode variables) throws IOException, InterruptedException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("query", query);
        if (variables != null) {
            requestBody.set("variables", variables);
        }
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            
            // Check for GraphQL errors
            if (responseJson.has("errors")) {
                JsonNode errors = responseJson.get("errors");
                if (errors.isArray() && errors.size() > 0) {
                    throw new IOException("GraphQL errors: " + errors.toString());
                }
            }
            
            return responseJson.get("data");
        } else {
            throw new IOException("Failed to execute GraphQL. Status code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() throws IOException, InterruptedException {
        logger.info("Getting all users...");
        
        String query = "query { users { id name email } }";
        
        JsonNode data = executeGraphQL(query, null);
        List<User> users = new ArrayList<>();
        
        JsonNode usersNode = data.get("users");
        for (JsonNode userNode : usersNode) {
            User user = new User();
            user.setId(userNode.get("id").asLong());
            user.setName(userNode.get("name").asText());
            user.setEmail(userNode.get("email").asText());
            users.add(user);
        }
        
        return users;
    }
    
    /**
     * Get a user by ID
     */
    public User getUserById(Long id) throws IOException, InterruptedException {
        logger.info("Getting user with ID: " + id);
        
        String query = "query GetUser($id: ID!) { userById(id: $id) { id name email } }";
        
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", id);
        
        JsonNode data = executeGraphQL(query, variables);
        JsonNode userNode = data.get("userById");
        
        if (userNode == null || userNode.isNull()) {
            logger.warning("User not found with ID: " + id);
            return null;
        }
        
        User user = new User();
        user.setId(userNode.get("id").asLong());
        user.setName(userNode.get("name").asText());
        user.setEmail(userNode.get("email").asText());
        
        return user;
    }
    
    /**
     * Create a new user
     */
    public User createUser(String name, String email) throws IOException, InterruptedException {
        logger.info("Creating user: " + name + " (" + email + ")");
        
        String query = "mutation CreateUser($input: UserInput!) { createUser(input: $input) { id name email } }";
        
        ObjectNode userInput = objectMapper.createObjectNode();
        userInput.put("name", name);
        userInput.put("email", email);
        
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", userInput);
        
        JsonNode data = executeGraphQL(query, variables);
        JsonNode userNode = data.get("createUser");
        
        User user = new User();
        user.setId(userNode.get("id").asLong());
        user.setName(userNode.get("name").asText());
        user.setEmail(userNode.get("email").asText());
        
        return user;
    }
    
    /**
     * Update an existing user
     */
    public User updateUser(Long id, String name, String email) throws IOException, InterruptedException {
        logger.info("Updating user with ID: " + id);
        
        String query = "mutation UpdateUser($id: ID!, $input: UserInput!) { updateUser(id: $id, input: $input) { id name email } }";
        
        ObjectNode userInput = objectMapper.createObjectNode();
        userInput.put("name", name);
        userInput.put("email", email);
        
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", id);
        variables.set("input", userInput);
        
        try {
            JsonNode data = executeGraphQL(query, variables);
            JsonNode userNode = data.get("updateUser");
            
            if (userNode == null || userNode.isNull()) {
                logger.warning("User not found with ID: " + id);
                return null;
            }
            
            User user = new User();
            user.setId(userNode.get("id").asLong());
            user.setName(userNode.get("name").asText());
            user.setEmail(userNode.get("email").asText());
            
            return user;
        } catch (IOException e) {
            if (e.getMessage().contains("not found")) {
                logger.warning("User not found with ID: " + id);
                return null;
            }
            throw e;
        }
    }
    
    /**
     * Delete a user
     */
    public boolean deleteUser(Long id) throws IOException, InterruptedException {
        logger.info("Deleting user with ID: " + id);
        
        String query = "mutation DeleteUser($id: ID!) { deleteUser(id: $id) }";
        
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", id);
        
        try {
            JsonNode data = executeGraphQL(query, variables);
            return data.get("deleteUser").asBoolean();
        } catch (IOException e) {
            if (e.getMessage().contains("not found")) {
                logger.warning("User not found with ID: " + id);
                return false;
            }
            throw e;
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
        GraphQLApiClient client = new GraphQLApiClient("http://localhost:8080/graphql");
        
        try {
            // Get all users to show current state
            List<User> users = client.getAllUsers();
            client.displayUsers(users);
            
            // Create a new user
            User createdUser = client.createUser("Alice Williams", "alice@example.com");
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
                User updatedUser = client.updateUser(userId, "Imran Gul", "imrangul@example.com");
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