-- AI Client Desktop Database Schema
-- SQLite database for storing chat sessions, messages, and image metadata

-- Chat Sessions Table
CREATE TABLE IF NOT EXISTS chat_sessions (
    id TEXT PRIMARY KEY,
    model_id TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT 'New Chat',
    created_at TEXT NOT NULL,
    last_message_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON chat_sessions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_last_message ON chat_sessions(last_message_at DESC);

-- Chat Messages Table
CREATE TABLE IF NOT EXISTS chat_messages (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_session ON chat_messages(session_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON chat_messages(timestamp DESC);

-- Generated Images Table
CREATE TABLE IF NOT EXISTS generated_images (
    id TEXT PRIMARY KEY,
    prompt TEXT NOT NULL,
    model_id TEXT NOT NULL,
    file_path TEXT NOT NULL,
    generated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_images_generated_at ON generated_images(generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_images_model ON generated_images(model_id);
