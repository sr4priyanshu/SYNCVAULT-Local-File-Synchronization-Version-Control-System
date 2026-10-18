PRAGMA foreign_keys = ON;
-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ============================================================
-- FOLDERS
-- ============================================================
CREATE TABLE IF NOT EXISTS folders (
    folder_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    folder_path TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
-- ============================================================
-- FILES
-- ============================================================
CREATE TABLE IF NOT EXISTS files (
    file_id INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id INTEGER NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL UNIQUE,
    extension TEXT,
    current_hash TEXT,
    current_size INTEGER,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT,
    FOREIGN KEY (folder_id)
        REFERENCES folders(folder_id)
        ON DELETE CASCADE
);
-- ============================================================
-- VERSIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS versions (
    version_id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    version_number INTEGER NOT NULL,
    content_hash TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    storage_type TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    parent_version_id INTEGER,
    FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE CASCADE,
    FOREIGN KEY (parent_version_id)
        REFERENCES versions(version_id)
);
-- ============================================================
-- ACTIVITY LOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS activity_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER,
    action TEXT NOT NULL,
    details TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE SET NULL
);
-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_folders_user
ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_files_folder
ON files(folder_id);
CREATE INDEX IF NOT EXISTS idx_files_name
ON files(file_name);
CREATE INDEX IF NOT EXISTS idx_files_hash
ON files(current_hash);
CREATE INDEX IF NOT EXISTS idx_versions_file
ON versions(file_id);
CREATE INDEX IF NOT EXISTS idx_versions_hash
ON versions(content_hash);
CREATE INDEX IF NOT EXISTS idx_versions_created
ON versions(created_at);
CREATE INDEX IF NOT EXISTS idx_activity_file
ON activity_logs(file_id);
CREATE INDEX IF NOT EXISTS idx_activity_created
ON activity_logs(created_at);