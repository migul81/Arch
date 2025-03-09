package com.example.grpcapi;

import com.example.grpcapi.service.UserServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class UserServer {
    private static final Logger logger = Logger.getLogger(UserServer.class.getName());
    
    private Server server;
    private final int port = 50051;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        final UserServer server = new UserServer();
        server.start();
        server.blockUntilShutdown();
    }
    
    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
            .addService(new UserServiceImpl())
            .build()
            .start();
        
        logger.info("Server started, listening on port " + port);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** Shutting down gRPC server due to JVM shutdown");
                try {
                    UserServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** Server shut down");
            }
        });
    }
    
    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
    
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
