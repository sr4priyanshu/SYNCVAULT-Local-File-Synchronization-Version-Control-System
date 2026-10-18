# SyncVault

**A Windows desktop application for file organization, version history, and backup management.**

SyncVault is being developed in Java with JavaFX and SQLite. The goal is to provide a desktop interface for managing tracked folders, keeping file-version metadata, and eventually backing up and restoring previous file versions.

> **Current status:** Phase 2 establishes the database layer and JavaFX interface. The screens and database operations are in place, but the automatic file-monitoring, backup, delta-storage, and actual file-restoration engine are planned for Phase 3.

## What SyncVault currently includes

- **Dashboard:** Displays tracked-folder, file, and version counts, recorded version sizes, and recent activity from the database.
- **Folder Manager:** Lets a user select folders, saves their paths in SQLite, prevents duplicate registrations, and records folder additions and removals in the activity log.
- **Version History:** Loads registered file records from the database and displays version metadata when versions exist.
- **Restore Center:** Lets a user select a registered file and inspect its stored version metadata. Actual restoration is not enabled yet.
- **SQLite data layer:** Manages users, tracked folders, file metadata, version metadata, and activity logs.
## Technology stack
- **Language:** Java 21
- **Desktop UI:** JavaFX
- **Database:** SQLite
- **Database connectivity:** SQLite JDBC
- **Build tool:** Maven
- **Target platform:** Windows
## Project structure
```
SyncVault/
├── pom.xml
├── .gitignore
├── database/
│   └── .gitkeep
├── docs/
│   └── schema.sql
└── src/
    ├── main/
    │   ├── java/com/syncvault/
    │   │   ├── App.java
    │   │   ├── db/
    │   │   │   ├── DatabaseManager.java
    │   │   │   ├── DatabaseInitializer.java
    │   │   │   ├── dao/
    │   │   │   │   ├── UserDao.java
    │   │   │   │   ├── FolderDao.java
    │   │   │   │   ├── FileDao.java
    │   │   │   │   ├── VersionDao.java
    │   │   │   │   ├── ActivityLogDao.java
    │   │   │   │   └── DashboardStatsDao.java
    │   │   │   └── models/
    │   │   │       ├── FileRecord.java
    │   │   │       ├── VersionRecord.java
    │   │   │       └── ActivityLog.java
    │   │   └── ui/
    │   │       ├── MainView.java
    │   │       ├── DashboardView.java
    │   │       ├── FolderManagerView.java
    │   │       ├── VersionHistoryView.java
    │   │       └── RestoreCenterView.java
    │   └── resources/
    │       └── schema.sql
    └── test/
        └── java/com/syncvault/db/
            └── DatabaseIntegrationTest.java
```

*The tree shows the intended current layout. Files may be added as development continues.*
## Prerequisites
Install the following before running SyncVault:
- JDK 21
- Apache Maven
- Git (optional, for source control)
Confirm Java and Maven are available in your terminal:
```powershell
java -version
mvn -version
```
## Run the application
1. Clone the repository and open the project folder in a terminal or VS Code.
2. Compile the project:
   ```powershell
   mvn clean compile
   ```
3. Run the database integration tests:
   ```powershell
   mvn clean test
   ```
4. Launch the JavaFX application:
   ```powershell
   mvn javafx:run
   ```
On startup, SyncVault initializes the SQLite schema from `src/main/resources/schema.sql` and uses `database/syncvault.db` as its default local database. The local database file is excluded from Git so each developer can create their own database at runtime.
## Database overview
SQLite currently contains five primary tables:

| Table | Purpose |
|---|---|
| `users` | Stores the local application user record. |
| `folders` | Stores folder paths registered in SyncVault. |
| `files` | Stores file names, paths, sizes, hashes, and related metadata. |
| `versions` | Stores version numbers, hashes, sizes, storage type/path, and parent-version references. |
| `activity_logs` | Stores application activity such as folder additions and removals. |
Database access is separated into DAO classes. Models represent database records, while `DatabaseManager` creates SQLite connections and `DatabaseInitializer` loads the schema.
## Current limitations
The present version is a Phase-2 foundation, not yet a complete automated backup application:
## Contributing
For team development, work on the branch assigned to your module and submit a Pull Request to the integration branch before changes reach `main`. Keep commits focused on one logical change, run `mvn clean test`, and never commit local database files, build output, credentials, or other secrets.
