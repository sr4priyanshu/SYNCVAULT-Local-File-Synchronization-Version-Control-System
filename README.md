# SyncVault — Local File Synchronization & Version Control System

SyncVault is a Java-based desktop application designed to monitor local files, track changes, maintain file versions, and restore previous versions. It uses a modular architecture with SQLite for metadata storage and delta-based versioning to manage file history efficiently.

## Features

- **File Monitoring:** Detects file creation, modification, and deletion.
- **File Scanning:** Scans directories and tracks file metadata.
- **Version Control:** Maintains multiple versions of files.
- **Delta Storage:** Stores file changes using block-level comparison.
- **Version Restoration:** Reconstructs and restores previous file versions.
- **Integrity Verification:** Uses hashing to verify file integrity.
- **Dashboard:** Provides an overview of tracked files and version activity.
- **Folder Management:** Allows users to manage monitored directories.

## Tech Stack

- **Language:** Java
- **UI:** JavaFX
- **Database:** SQLite
- **Build Tool:** Maven
- **File Monitoring:** Java WatchService
- **Versioning:** Hash-based comparison and delta storage

## Project Architecture

```text
SyncVault/
├── src/main/java/com/syncvault/
│   ├── App.java
│   ├── db/                 # Database, DAOs and models
│   ├── services/           # Monitoring and versioning logic
│   │   └── delta/          # Delta generation and reconstruction
│   └── ui/                 # JavaFX interface
├── src/main/resources/     # Application resources
├── src/test/               # Unit and integration tests
├── docs/                   # Database schema and documentation
└── pom.xml                 # Maven configuration
```

## Getting Started

### Prerequisites
- Java JDK 21 or compatible project-configured JDK
- Apache Maven
- Git
### Installation

```bash
git clone https://github.com/sr4priyanshu/SYNCVAULT-Local-File-Synchronization-Version-Control-System.git
cd SYNCVAULT-Local-File-Synchronization-Version-Control-System
```
### Build the Project
```bash
mvn clean verify
```
### Run the Application
```bash
mvn javafx:run
```

## Project Team

| Member | Responsibility |
|---|---|
| Priyanshu Singh | Database Design & Implementation |
| Arjit Mishra | File Monitoring & Scanning |
| Aarzu Manhas | Versioning & Delta Management |
| Saundrya Arora | JavaFX UI Development |

## Project Status

Developed as an academic project at **Graphic Era Deemed to be University**, under the B.Tech Computer Science and Engineering programme.

---

**SyncVault — Track. Version. Restore.**
