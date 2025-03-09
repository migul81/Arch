package com.example.client;

import com.example.eventapi.dto.UpdateUserRequest;
import com.example.eventapi.dto.UserDTO;
import com.example.eventapi.dto.UserIdRequest;
import com.example.eventapi.event.*;
import com.example.eventapi.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class EventApiClient {
	private static final Logger logger = Logger.getLogger(EventApiClient.class.getName());
	private final WebSocketStompClient stompClient;
	private StompSession stompSession;
	private final String serverUrl;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public EventApiClient(String serverUrl) {
		this.serverUrl = serverUrl;

		// Set up WebSocket client
		List<Transport> transports = new ArrayList<>();
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		SockJsClient sockJsClient = new SockJsClient(transports);

		this.stompClient = new WebSocketStompClient(sockJsClient);
		this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
	}

	/**
	 * Connect to the WebSocket server
	 */
	public void connect() throws InterruptedException, ExecutionException, TimeoutException {
		logger.info("Connecting to WebSocket server at " + serverUrl);

		final CountDownLatch connectLatch = new CountDownLatch(1);

		StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				logger.info("Connected to WebSocket server");
				connectLatch.countDown();
			}

			@Override
			public void handleException(StompSession session, StompCommand command, StompHeaders headers,
					byte[] payload, Throwable exception) {
				logger.severe("Error: " + exception.getMessage());
				exception.printStackTrace();
			}
		};

		stompSession = stompClient.connect(serverUrl, new WebSocketHttpHeaders(), sessionHandler).get(5,
				TimeUnit.SECONDS);

		// Wait for connection to be established
		connectLatch.await(5, TimeUnit.SECONDS);

		// Subscribe to user events
		// Create a custom message handler that properly handles the JSON response
		stompSession.subscribe("/topic/users", new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class; // Deserialize as a generic Map instead of String
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				try {
					// Cast the payload to a Map
					@SuppressWarnings("unchecked")
					Map<String, Object> eventMap = (Map<String, Object>) payload;

					// Get the event type
					String eventType = (String) eventMap.get("type");
					logger.info("Received event: " + eventType);

					switch (eventType) {
					case "USER_LIST":
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> userMaps = (List<Map<String, Object>>) eventMap.get("users");
						List<User> users = new ArrayList<>();

						for (Map<String, Object> userMap : userMaps) {
							User user = new User();
							user.setId(((Number) userMap.get("id")).longValue());
							user.setName((String) userMap.get("name"));
							user.setEmail((String) userMap.get("email"));
							users.add(user);
						}

						displayUsers(users);
						break;

					case "USER_CREATED":
					case "USER_UPDATED":
					case "GET_USER":
						@SuppressWarnings("unchecked")
						Map<String, Object> userMap = (Map<String, Object>) eventMap.get("user");
						User user = new User();
						user.setId(((Number) userMap.get("id")).longValue());
						user.setName((String) userMap.get("name"));
						user.setEmail((String) userMap.get("email"));

						if (eventType.equals("USER_CREATED")) {
							logger.info("User created: " + user.getName() + " (" + user.getEmail() + ")");
						} else if (eventType.equals("USER_UPDATED")) {
							logger.info("User updated: " + user.getName() + " (" + user.getEmail() + ")");
						} else {
							logger.info("User details: " + user.getName() + " (" + user.getEmail() + ")");
						}
						break;

					case "USER_DELETED":
						Number userId = (Number) eventMap.get("userId");
						logger.info("User deleted: ID " + userId.longValue());
						break;

					case "ERROR":
						String message = (String) eventMap.get("message");
						Number code = (Number) eventMap.get("code");
						logger.severe("Error (" + code + "): " + message);
						break;

					default:
						logger.warning("Unknown event type: " + eventType);
					}
				} catch (Exception e) {
					logger.severe("Error handling message: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Display a list of users
	 */
	private void displayUsers(List<User> users) {
		logger.info("=== Users ===");
		for (User user : users) {
			logger.info(user.getId() + ": " + user.getName() + " (" + user.getEmail() + ")");
		}
		logger.info("============");
	}

	/**
	 * Get all users
	 */
	public void getAllUsers() {
		if (isConnected()) {
			logger.info("Requesting all users...");
			Map<String, Object> emptyRequest = new HashMap<>();
			stompSession.send("/app/users.getAll", emptyRequest);
		} else {
			logger.warning("Not connected to WebSocket server");
		}
	}

	/**
	 * Get a user by ID
	 */
	public void getUser(Long id) {
		if (isConnected()) {
			logger.info("Requesting user with ID: " + id);
			UserIdRequest request = new UserIdRequest(id);
			stompSession.send("/app/users.get", request);
		} else {
			logger.warning("Not connected to WebSocket server");
		}
	}

	/**
	 * Create a new user
	 */
	public void createUser(String name, String email) {
		if (isConnected()) {
			logger.info("Creating user: " + name + " (" + email + ")");
			UserDTO userDTO = new UserDTO(name, email);
			stompSession.send("/app/users.create", userDTO);
		} else {
			logger.warning("Not connected to WebSocket server");
		}
	}

	/**
	 * Update an existing user
	 */
	public void updateUser(Long id, String name, String email) {
		if (isConnected()) {
			logger.info("Updating user with ID: " + id);
			UserDTO userDTO = new UserDTO(name, email);
			UpdateUserRequest request = new UpdateUserRequest(id, userDTO);
			stompSession.send("/app/users.update", request);
		} else {
			logger.warning("Not connected to WebSocket server");
		}
	}

	/**
	 * Delete a user
	 */
	public void deleteUser(Long id) {
		if (isConnected()) {
			logger.info("Deleting user with ID: " + id);
			UserIdRequest request = new UserIdRequest(id);
			stompSession.send("/app/users.delete", request);
		} else {
			logger.warning("Not connected to WebSocket server");
		}
	}

	/**
	 * Check if connected to WebSocket server
	 */
	private boolean isConnected() {
		return stompSession != null && stompSession.isConnected();
	}

	/**
	 * Disconnect from WebSocket server
	 */
	public void disconnect() {
		if (isConnected()) {
			stompSession.disconnect();
			logger.info("Disconnected from WebSocket server");
		}
	}

	/**
	 * Main method to run the client
	 */
	public static void main(String[] args) {
		EventApiClient client = new EventApiClient("ws://localhost:8080/ws");

		try {
			// Connect to server
			client.connect();

			// Get all users to show current state
			client.getAllUsers();

			// Wait a moment for data to load
			Thread.sleep(1000);

			// Create a new user
			client.createUser("Imran Gul", "imrangul@example.com");

			// Wait for creation to complete
			Thread.sleep(1000);

			// Get all users to see the new user
			client.getAllUsers();

			// Wait a moment
			Thread.sleep(1000);

			// Update user with ID 2 (assuming it exists)
			client.updateUser(2L, "Jane Wilson", "jane.wilson@example.com");

			// Wait for update to complete
			Thread.sleep(1000);

			// Get the updated user
			client.getUser(2L);

			// Wait a moment
			Thread.sleep(1000);

			// Delete user with ID 3 (assuming it exists)
			client.deleteUser(3L);

			// Wait for deletion to complete
			Thread.sleep(1000);

			// Get all users to see the result
			client.getAllUsers();

			// Wait a moment for data to load
			Thread.sleep(1000);

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

				if (command.equals("list")) {
					client.getAllUsers();
				} else if (command.startsWith("get ")) {
					String[] parts = command.split(" ", 2);
					if (parts.length > 1) {
						try {
							Long id = Long.parseLong(parts[1]);
							client.getUser(id);
						} catch (NumberFormatException e) {
							System.out.println("Invalid ID format");
						}
					}
				} else if (command.startsWith("create ")) {
					String[] parts = command.split(" ", 3);
					if (parts.length > 2) {
						client.createUser(parts[1], parts[2]);
					} else {
						System.out.println("Usage: create [name] [email]");
					}
				} else if (command.startsWith("update ")) {
					String[] parts = command.split(" ", 4);
					if (parts.length > 3) {
						try {
							Long id = Long.parseLong(parts[1]);
							client.updateUser(id, parts[2], parts[3]);
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
							client.deleteUser(id);
						} catch (NumberFormatException e) {
							System.out.println("Invalid ID format");
						}
					}
				} else if (!command.equals("exit")) {
					System.out.println("Unknown command: " + command);
				}

				// Wait a moment for response
				Thread.sleep(500);
			}

			scanner.close();

		} catch (Exception e) {
			logger.severe("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Disconnect
			client.disconnect();
		}
	}
}