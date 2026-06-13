package com.aiclient.adapter.output.persistence;

import com.aiclient.domain.model.GeneratedImage;
import com.aiclient.domain.port.output.ImagePersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite implementation of ImagePersistencePort.
 * Provides persistent storage for generated image metadata.
 */
public class SQLiteImagePersistenceAdapter implements ImagePersistencePort {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteImagePersistenceAdapter.class);

    private final DatabaseManager databaseManager;

    public SQLiteImagePersistenceAdapter(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager cannot be null");
        this.databaseManager.initialize();
    }

    @Override
    public GeneratedImage saveImage(GeneratedImage image) {
        Objects.requireNonNull(image, "Image cannot be null");

        String sql = "INSERT OR REPLACE INTO generated_images (id, prompt, model_id, file_path, generated_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, image.getId());
                stmt.setString(2, image.getPrompt());
                stmt.setString(3, image.getModelId());
                stmt.setString(4, image.getFilePath().toString());
                stmt.setString(5, image.getGeneratedAt().toString());

                stmt.executeUpdate();
                logger.debug("Saved image metadata: {}", image.getId());
                return image;
            }
        } catch (SQLException e) {
            logger.error("Failed to save image {}: {}", image.getId(), e.getMessage());
            throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<GeneratedImage> findImageById(String imageId) {
        Objects.requireNonNull(imageId, "Image ID cannot be null");

        String sql = "SELECT id, prompt, model_id, file_path, generated_at FROM generated_images WHERE id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, imageId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        GeneratedImage image = mapResultSetToImage(rs);
                        logger.debug("Found image: {}", imageId);
                        return Optional.of(image);
                    }
                }

                logger.debug("Image not found: {}", imageId);
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Failed to find image {}: {}", imageId, e.getMessage());
            throw new RuntimeException("Failed to find image: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneratedImage> findAllImages() {
        String sql = "SELECT id, prompt, model_id, file_path, generated_at FROM generated_images " +
                "ORDER BY generated_at DESC";

        List<GeneratedImage> images = new ArrayList<>();

        try {
            Connection conn = databaseManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    images.add(mapResultSetToImage(rs));
                }

                logger.debug("Found {} images", images.size());
                return images;
            }
        } catch (SQLException e) {
            logger.error("Failed to find all images: {}", e.getMessage());
            throw new RuntimeException("Failed to find images: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String imageId) {
        Objects.requireNonNull(imageId, "Image ID cannot be null");

        String sql = "DELETE FROM generated_images WHERE id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, imageId);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    logger.info("Deleted image metadata: {}", imageId);
                } else {
                    logger.warn("Image not found for deletion: {}", imageId);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to delete image {}: {}", imageId, e.getMessage());
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    private GeneratedImage mapResultSetToImage(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String prompt = rs.getString("prompt");
        String modelId = rs.getString("model_id");
        Path filePath = Paths.get(rs.getString("file_path"));
        LocalDateTime generatedAt = LocalDateTime.parse(rs.getString("generated_at"));

        return new GeneratedImage(id, prompt, modelId, filePath, generatedAt);
    }
}
