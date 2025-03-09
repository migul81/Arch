package com.example.grpcapi;

import com.example.grpcapi.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserClient {
    private static final Logger logger = Logger.getLogger(UserClient.class.getName());
    
    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    
    public UserClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
    }
    
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    
    public void getAllUsers() {
        logger.info("Getting all users...");
        Empty request = Empty.newBuilder().build();
        
        try {
            UserList response = blockingStub.getAllUsers(request);
            logger.info("Found " + response.getUsersCount() + " users");
            
            for (User user : response.getUsersList()) {
                logger.info(user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }
    
    public void getUser(long id) {
        logger.info("Getting user with ID: " + id);
        UserRequest request = UserRequest.newBuilder().setId(id).build();
        
        try {
            User response = blockingStub.getUser(request);
            logger.info("Found user: " + response.getId() + ": " + response.getName() + " (" + response.getEmail() + ")");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }
    
    public void createUser(String name, String email) {
        logger.info("Creating user: " + name);
        User request = User.newBuilder()
            .setName(name)
            .setEmail(email)
            .build();
        
        try {
            User response = blockingStub.createUser(request);
            logger.info("Created user: " + response.getId() + ": " + response.getName() + " (" + response.getEmail() + ")");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }
    
    public void updateUser(long id, String name, String email) {
        logger.info("Updating user with ID: " + id);
        UpdateUserRequest request = UpdateUserRequest.newBuilder()
            .setId(id)
            .setUser(User.newBuilder()
                .setName(name)
                .setEmail(email)
                .build())
            .build();
        
        try {
            User response = blockingStub.updateUser(request);
            logger.info("Updated user: " + response.getId() + ": " + response.getName() + " (" + response.getEmail() + ")");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }
    
    public void deleteUser(long id) {
        logger.info("Deleting user with ID: " + id);
        UserRequest request = UserRequest.newBuilder().setId(id).build();
        
        try {
            blockingStub.deleteUser(request);
            logger.info("User deleted successfully");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }
    
    public static void main(String[] args) throws Exception {
        UserClient client = new UserClient("localhost", 50051);
        
        try {
            // Print all users
            client.getAllUsers();
            
            // Get a specific user
            client.getUser(1);
            
            // Create a new user
            client.createUser("Alice Williams", "alice@example.com");
            
            // Show all users after creation
            client.getAllUsers();
            
            // Update a user
            client.updateUser(2, "Jane Wilson", "jane.wilson@example.com");
            
            // Show the updated user
            client.getUser(2);
            
            // Delete a user
            client.deleteUser(3);
            
            // Show all users after deletion
            client.getAllUsers();
            
        } finally {
            client.shutdown();
        }
    }
}
