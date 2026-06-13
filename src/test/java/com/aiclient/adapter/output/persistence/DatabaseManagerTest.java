package com.aiclient.adapter.output.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private String dbPath;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test.db").toString();
        databaseManager = new DatabaseManager(dbPath);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    void shouldCreateDatabaseManager() {
        assertThat(databaseManager).isNotNull();
    }

    @Test
    void shouldCreateDefaultDatabaseManager() {
        DatabaseManager defaultManager = new DatabaseManager();
        assertThat(defaultManager).isNotNull();
        defaultManager.close();
    }

    @Test
    void shouldRejectNullDatabasePath() {
        assertThatThrownBy(() -> new DatabaseManager(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Database path cannot be null");
    }

    @Test
    void shouldGetConnection() throws Exception {
        databaseManager.initialize();
        Connection conn = databaseManager.getConnection();
        assertThat(conn).isNotNull();
        assertThat(conn.isClosed()).isFalse();
    }

    @Test
    void shouldReuseConnection() throws Exception {
        databaseManager.initialize();
        Connection conn1 = databaseManager.getConnection();
        Connection conn2 = databaseManager.getConnection();
        assertThat(conn1).isSameAs(conn2);
    }

    @Test
    void shouldInitializeDatabase() {
        databaseManager.initialize();
        assertThat(Files.exists(Path.of(dbPath))).isTrue();
    }

    @Test
    void shouldCreateTables() throws Exception {
        databaseManager.initialize();
        Connection conn = databaseManager.getConnection();

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
            );

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("chat_messages");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("chat_sessions");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("generated_images");
        }
    }

    @Test
    void shouldCreateIndexes() throws Exception {
        databaseManager.initialize();
        Connection conn = databaseManager.getConnection();

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%' ORDER BY name"
            );

            int indexCount = 0;
            while (rs.next()) {
                indexCount++;
            }

            assertThat(indexCount).isGreaterThanOrEqualTo(5);
        }
    }

    @Test
    void shouldCloseConnection() throws Exception {
        databaseManager.initialize();
        Connection conn = databaseManager.getConnection();
        assertThat(conn.isClosed()).isFalse();

        databaseManager.close();
        assertThat(conn.isClosed()).isTrue();
    }

    @Test
    void shouldHandleMultipleClose() {
        databaseManager.initialize();
        databaseManager.close();
        databaseManager.close();
    }

    @Test
    void shouldHandleCloseWithoutInitialize() {
        databaseManager.close();
    }
}
