package com.aiclient.adapter.input.ui;

import com.aiclient.domain.model.ChatMessage;
import com.aiclient.domain.model.ChatSession;
import com.aiclient.domain.model.GeneratedImage;
import com.aiclient.domain.port.input.ChatUseCase;
import com.aiclient.domain.port.input.ImageGenerationUseCase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller for browsing chat and image history.
 * Provides a unified interface for viewing and managing historical data.
 */
public class HistoryController {

    private final ChatUseCase chatService;
    private final ImageGenerationUseCase imageService;

    @FXML
    private RadioButton chatModeRadio;

    @FXML
    private RadioButton imageModeRadio;

    @FXML
    private ListView<Object> historyList;

    @FXML
    private VBox detailsPane;

    @FXML
    private Button deleteButton;

    private Object selectedItem;
    private boolean isChatMode = true;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor with dependency injection.
     *
     * @param chatService Chat use case implementation
     * @param imageService Image generation use case implementation
     */
    public HistoryController(ChatUseCase chatService, ImageGenerationUseCase imageService) {
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.imageService = Objects.requireNonNull(imageService, "ImageService cannot be null");
    }

    /**
     * JavaFX lifecycle method called after FXML loading.
     * Initializes the UI state and loads chat sessions by default.
     */
    @FXML
    public void initialize() {
        // Configure history list cell factory
        historyList.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof ChatSession) {
                    ChatSession session = (ChatSession) item;
                    setText(session.getTitle() + " - " + session.getCreatedAt().format(DATE_FORMATTER));
                } else if (item instanceof GeneratedImage) {
                    GeneratedImage image = (GeneratedImage) item;
                    String preview = image.getPrompt().length() > 50
                            ? image.getPrompt().substring(0, 50) + "..."
                            : image.getPrompt();
                    setText(preview + " - " + image.getGeneratedAt().format(DATE_FORMATTER));
                }
            }
        });

        loadChatSessions();
    }

    /**
     * Handles selection of Chat mode radio button.
     * Loads chat sessions into the history list.
     */
    @FXML
    private void handleChatModeSelected() {
        isChatMode = true;
        clearDetails();
        loadChatSessions();
    }

    /**
     * Handles selection of Image mode radio button.
     * Loads generated images into the history list.
     */
    @FXML
    private void handleImageModeSelected() {
        isChatMode = false;
        clearDetails();
        loadGeneratedImages();
    }

    /**
     * Handles item selection in the history list.
     * Displays details for the selected item.
     */
    @FXML
    private void handleItemSelected() {
        Object item = historyList.getSelectionModel().getSelectedItem();
        if (item != null) {
            selectedItem = item;
            deleteButton.setDisable(false);
            displayDetails(item);
        }
    }

    /**
     * Handles the Delete button action.
     * Confirms deletion and removes the selected item.
     */
    @FXML
    private void handleDelete() {
        if (selectedItem == null) {
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this item?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Item");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            deleteSelectedItem();
        }
    }

    /**
     * Loads chat sessions into the history list.
     */
    private void loadChatSessions() {
        new Thread(() -> {
            try {
                List<ChatSession> sessions = chatService.getAllSessions();
                Platform.runLater(() -> {
                    historyList.getItems().clear();
                    historyList.getItems().addAll(sessions);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load chat sessions: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Loads generated images into the history list.
     */
    private void loadGeneratedImages() {
        new Thread(() -> {
            try {
                List<GeneratedImage> images = imageService.getAllImages();
                Platform.runLater(() -> {
                    historyList.getItems().clear();
                    historyList.getItems().addAll(images);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load generated images: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Displays details for the selected item in the details pane.
     *
     * @param item Selected chat session or generated image
     */
    private void displayDetails(Object item) {
        detailsPane.getChildren().clear();

        if (item instanceof ChatSession) {
            displaySessionDetails((ChatSession) item);
        } else if (item instanceof GeneratedImage) {
            displayImageDetails((GeneratedImage) item);
        }
    }

    /**
     * Displays chat session details including all messages.
     *
     * @param session Chat session to display
     */
    private void displaySessionDetails(ChatSession session) {
        Label titleLabel = new Label("Session: " + session.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label infoLabel = new Label(
                "Model: " + session.getModelId() + "\n" +
                "Created: " + session.getCreatedAt().format(DATE_FORMATTER)
        );

        Separator separator = new Separator();

        Label messagesLabel = new Label("Messages:");
        messagesLabel.setStyle("-fx-font-weight: bold;");

        detailsPane.getChildren().addAll(titleLabel, infoLabel, separator, messagesLabel);

        // Load messages in background
        new Thread(() -> {
            try {
                List<ChatMessage> messages = chatService.getSessionHistory(session.getId());
                Platform.runLater(() -> {
                    for (ChatMessage msg : messages) {
                        VBox messageBox = new VBox(5);
                        messageBox.setPadding(new Insets(5));
                        messageBox.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");

                        Label roleLabel = new Label(msg.getRole().toString());
                        roleLabel.setStyle("-fx-font-weight: bold;");

                        Label contentLabel = new Label(msg.getContent());
                        contentLabel.setWrapText(true);

                        messageBox.getChildren().addAll(roleLabel, contentLabel);
                        detailsPane.getChildren().add(messageBox);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load messages: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Displays generated image details including preview.
     *
     * @param image Generated image to display
     */
    private void displayImageDetails(GeneratedImage image) {
        Label titleLabel = new Label("Generated Image");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label infoLabel = new Label(
                "Model: " + image.getModelId() + "\n" +
                "Generated: " + image.getGeneratedAt().format(DATE_FORMATTER)
        );

        Label promptLabel = new Label("Prompt:");
        promptLabel.setStyle("-fx-font-weight: bold;");

        Label promptText = new Label(image.getPrompt());
        promptText.setWrapText(true);

        Separator separator = new Separator();

        detailsPane.getChildren().addAll(titleLabel, infoLabel, separator, promptLabel, promptText);

        // Load and display image
        try {
            String imagePath = image.getFilePath().toAbsolutePath().toString();
            Image fxImage = new Image("file:///" + imagePath);
            ImageView imageView = new ImageView(fxImage);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(400);
            detailsPane.getChildren().add(imageView);
        } catch (Exception e) {
            Label errorLabel = new Label("Failed to load image: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            detailsPane.getChildren().add(errorLabel);
        }
    }

    /**
     * Deletes the currently selected item.
     */
    private void deleteSelectedItem() {
        if (selectedItem == null) {
            return;
        }

        new Thread(() -> {
            try {
                if (selectedItem instanceof ChatSession) {
                    ChatSession session = (ChatSession) selectedItem;
                    chatService.deleteSession(session.getId());
                } else if (selectedItem instanceof GeneratedImage) {
                    GeneratedImage image = (GeneratedImage) selectedItem;
                    imageService.deleteImage(image.getId());
                }

                Platform.runLater(() -> {
                    clearDetails();
                    selectedItem = null;
                    deleteButton.setDisable(true);

                    // Refresh the list
                    if (isChatMode) {
                        loadChatSessions();
                    } else {
                        loadGeneratedImages();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to delete item: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Clears the details pane.
     */
    private void clearDetails() {
        detailsPane.getChildren().clear();
        Label placeholder = new Label("Select an item to view details");
        placeholder.setStyle("-fx-text-fill: gray;");
        detailsPane.getChildren().add(placeholder);
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
