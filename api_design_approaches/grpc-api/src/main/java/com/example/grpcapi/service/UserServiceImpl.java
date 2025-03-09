package com.example.grpcapi.service;

import com.example.grpcapi.model.User;
import com.example.grpcapi.proto.Empty;
import com.example.grpcapi.proto.UserList;
import com.example.grpcapi.proto.UserRequest;
import com.example.grpcapi.proto.UpdateUserRequest;
import com.example.grpcapi.proto.UserServiceGrpc;
import com.example.grpcapi.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Optional;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    private final UserRepository userRepository;
    
    public UserServiceImpl() {
        this.userRepository = new UserRepository();
    }
    
    @Override
    public void getAllUsers(Empty request, StreamObserver<UserList> responseObserver) {
        try {
            List<User> users = userRepository.findAll();
            
            // Convert model users to proto users
            UserList.Builder userListBuilder = UserList.newBuilder();
            for (User user : users) {
                userListBuilder.addUsers(convertToProtoUser(user));
            }
            
            // Send the response
            responseObserver.onNext(userListBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
    
    @Override
    public void getUser(UserRequest request, StreamObserver<com.example.grpcapi.proto.User> responseObserver) {
        try {
            Optional<User> userOpt = userRepository.findById(request.getId());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                responseObserver.onNext(convertToProtoUser(user));
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("User not found with id: " + request.getId())
                        .asRuntimeException()
                );
            }
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
    
    @Override
    public void createUser(com.example.grpcapi.proto.User request, 
                          StreamObserver<com.example.grpcapi.proto.User> responseObserver) {
        try {
            // Convert proto user to model user
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            
            // Save the user
            User savedUser = userRepository.save(user);
            
            // Return the saved user
            responseObserver.onNext(convertToProtoUser(savedUser));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to create user: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
    
    @Override
    public void updateUser(UpdateUserRequest request, 
                          StreamObserver<com.example.grpcapi.proto.User> responseObserver) {
        try {
            Long userId = request.getId();
            Optional<User> existingUserOpt = userRepository.findById(userId);
            
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                
                // Update fields
                existingUser.setName(request.getUser().getName());
                existingUser.setEmail(request.getUser().getEmail());
                
                // Save the updated user
                User updatedUser = userRepository.save(existingUser);
                
                // Return the updated user
                responseObserver.onNext(convertToProtoUser(updatedUser));
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("User not found with id: " + userId)
                        .asRuntimeException()
                );
            }
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to update user: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
    
    @Override
    public void deleteUser(UserRequest request, StreamObserver<Empty> responseObserver) {
        try {
            Long userId = request.getId();
            Optional<User> existingUserOpt = userRepository.findById(userId);
            
            if (existingUserOpt.isPresent()) {
                userRepository.deleteById(userId);
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("User not found with id: " + userId)
                        .asRuntimeException()
                );
            }
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to delete user: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
    
    // Helper method to convert model User to proto User
    private com.example.grpcapi.proto.User convertToProtoUser(User user) {
        return com.example.grpcapi.proto.User.newBuilder()
            .setId(user.getId())
            .setName(user.getName())
            .setEmail(user.getEmail())
            .build();
    }
}