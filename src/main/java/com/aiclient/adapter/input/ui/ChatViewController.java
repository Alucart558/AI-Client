package com.aiclient.adapter.input.ui;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.port.input.ChatUseCase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller for the chat interface.
 * Manages chat sessions and message interactions with the AI.
 */
public class ChatViewController {

    private final ChatUseCase chatService;

    @FXML
    private ComboBox<ChatSession> sessionSelector;

    @FXML
    private Button newSessionButton;

    @FXML
    private Button deleteSessionButton;

    @FXML
    private ListView<String> messageList;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    private ChatSession currentSession;

    /**
     * Constructor with dependency injection.
     *
     * @param chatService Chat use case implementation
     */
    public ChatViewController(ChatUseCase chatService) {
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
    }

    /**
     * JavaFX lifecycle method called after FXML loading.
     * Initializes the UI state and loads existing sessions.
     */
    @FXML
    public void initialize() {
        // Configure session selector to display session titles
        sessionSelector.setCellFactory(lv -> new ListCell<ChatSession>() {
            @Override
            protected void updateItem(ChatSession session, boolean empty) {
                super.updateItem(session, empty);
                setText(empty || session == null ? null : session.getTitle());
            }
        });

        sessionSelector.setButtonCell(new ListCell<ChatSession>() {
            @Override
            protected void updateItem(ChatSession session, boolean empty) {
                super.updateItem(session, empty);
                setText(empty || session == null ? "Select a session" : session.getTitle());
            }
        });

        // Disable send button initially
        sendButton.setDisable(true);
        deleteSessionButton.setDisable(true);

        // Enable send button only when session is selected and input is not empty
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> updateSendButtonState());

        // Enable delete button only when session is selected
        sessionSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateDeleteButtonState();
        });

        refreshSessions();
    }

    /**
     * Handles the New Session button action.
     * Prompts user for model selection and creates a new chat session.
     */
    @FXML
    private void handleNewSession() {
        new Thread(() -> {
            try {
                List<String> models = chatService.getAvailableModels();

                Platform.runLater(() -> {
                    if (models.isEmpty()) {
                        showError("Cannot fetch models from Ollama. Is Ollama running?");
                        return;
                    }

                    ChoiceDialog<String> dialog = new ChoiceDialog<>(models.get(0), models);
                    dialog.setTitle("New Chat Session");
                    dialog.setHeaderText("Create a new chat session");
                    dialog.setContentText("Select model:");

                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(modelId -> {
                        new Thread(() -> {
                            try {
                                ChatSession session = chatService.createSession(modelId);
                                Platform.runLater(() -> {
                                    refreshSessions();
                                    sessionSelector.getSelectionModel().select(session);
                                    currentSession = session;
                                    refreshMessages();
                                    updateSendButtonState();
                                });
                            } catch (IllegalArgumentException e) {
                                Platform.runLater(() -> {
                                    showError("Invalid model: " + e.getMessage());
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> showError("Failed to create session: " + e.getMessage()));
                            }
                        }).start();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load models: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Handles the Send button action.
     * Sends the user's message and displays the AI response.
     */
    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentSession == null) {
            return;
        }

        String sessionId = currentSession.getId();

        // Add user message to display immediately
        messageList.getItems().add("USER: " + content);
        messageInput.clear();
        sendButton.setDisable(true);

        // Scroll to bottom
        scrollToBottom();

        // Send message in background thread
        new Thread(() -> {
            try {
                ChatMessage response = chatService.sendMessage(sessionId, content);
                Platform.runLater(() -> {
                    messageList.getItems().add("ASSISTANT: " + response.getContent());
                    scrollToBottom();
                    updateSendButtonState();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to send message: " + e.getMessage());
                    updateSendButtonState();
                });
            }
        }).start();
    }

    /**
     * Handles the Delete Session button action.
     * Prompts for confirmation and deletes the current session.
     */
    @FXML
    private void handleDeleteSession() {
        if (currentSession == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Session");
        confirmation.setHeaderText("Delete this chat session?");
        confirmation.setContentText("This will permanently delete: " + currentSession.getTitle());

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sessionId = currentSession.getId();

            new Thread(() -> {
                try {
                    chatService.deleteSession(sessionId);
                    Platform.runLater(() -> {
                        currentSession = null;
                        refreshSessions();
                        messageList.getItems().clear();
                        updateDeleteButtonState();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to delete session: " + e.getMessage()));
                }
            }).start();
        }
    }

    /**
     * Handles session selection from the ComboBox.
     * Loads messages for the selected session.
     */
    @FXML
    private void handleSessionSelected() {
        ChatSession selected = sessionSelector.getSelectionModel().getSelectedItem();
        if (selected != null) {
            currentSession = selected;
            refreshMessages();
            updateSendButtonState();
            updateDeleteButtonState();
        }
    }

    /**
     * Refreshes the session list from the database.
     */
    private void refreshSessions() {
        new Thread(() -> {
            try {
                List<ChatSession> sessions = chatService.getAllSessions();
                Platform.runLater(() -> {
                    sessionSelector.getItems().clear();
                    sessionSelector.getItems().addAll(sessions);
                    if (!sessions.isEmpty() && currentSession == null) {
                        sessionSelector.getSelectionModel().selectFirst();
                        currentSession = sessions.get(0);
                        refreshMessages();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load sessions: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Refreshes the message list for the current session.
     */
    private void refreshMessages() {
        if (currentSession == null) {
            messageList.getItems().clear();
            return;
        }

        new Thread(() -> {
            try {
                List<ChatMessage> messages = chatService.getSessionHistory(currentSession.getId());
                Platform.runLater(() -> {
                    messageList.getItems().clear();
                    for (ChatMessage msg : messages) {
                        String roleLabel = msg.getRole().toString();
                        messageList.getItems().add(roleLabel + ": " + msg.getContent());
                    }
                    scrollToBottom();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load messages: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Updates the send button state based on input and session selection.
     */
    private void updateSendButtonState() {
        boolean hasSession = currentSession != null;
        boolean hasText = !messageInput.getText().trim().isEmpty();
        sendButton.setDisable(!hasSession || !hasText);
    }

    /**
     * Updates the delete button state based on session selection.
     */
    private void updateDeleteButtonState() {
        deleteSessionButton.setDisable(currentSession == null);
    }

    /**
     * Scrolls the message list to the bottom.
     */
    private void scrollToBottom() {
        if (!messageList.getItems().isEmpty()) {
            messageList.scrollTo(messageList.getItems().size() - 1);
        }
    }

    /**
     * Displays an error alert dialog.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
