package com.aiclient.adapter.output.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages SQLite database connections and initialization.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DEFAULT_DB_PATH = "ai-client.db";
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final String databasePath;
    private Connection connection;

    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseManager(String databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "Database path cannot be null");
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            logger.debug("Creating new database connection: {}", databasePath);
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public void initialize() {
        try {
            ensureDatabaseFileExists();
            Connection conn = getConnection();
            executeSchema(conn);
            logger.info("Database initialized successfully: {}", databasePath);
        } catch (SQLException | IOException e) {
            logger.error("Failed to initialize database: {}", e.getMessage());
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.debug("Database connection closed");
            } catch (SQLException e) {
                logger.warn("Error closing database connection: {}", e.getMessage());
            }
        }
    }

    private void ensureDatabaseFileExists() throws IOException {
        Path dbPath = Paths.get(databasePath);
        if (!Files.exists(dbPath)) {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            logger.debug("Database file will be created: {}", databasePath);
        }
    }

    private void executeSchema(Connection conn) throws SQLException, IOException {
        String schema = loadSchemaFromResource();

        try (Statement stmt = conn.createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            logger.debug("Database schema executed successfully");
        }
    }

    private String loadSchemaFromResource() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IOException("Schema resource not found: " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
