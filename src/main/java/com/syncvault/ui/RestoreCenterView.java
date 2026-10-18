package com.syncvault.ui;
import com.syncvault.db.dao.FileDao;
import com.syncvault.db.dao.UserDao;
import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.FileRecord;
import com.syncvault.db.models.VersionRecord;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class RestoreCenterView extends BorderPane {
    private static final String BACKGROUND = "#0B1120";
    private static final String CARD = "#111827";
    private static final String BORDER = "#1F2937";
    private static final String TEXT = "#E5E7EB";
    private static final String MUTED = "#9CA3AF";
    private static final String ACCENT = "#6366F1";
    private static final String GREEN = "#34D399";
    private static final String WARNING = "#F59E0B";
    private final FileDao fileDao = new FileDao();
    private final UserDao userDao = new UserDao();
    private final VersionDao versionDao = new VersionDao();
    private final ComboBox<FileRecord> fileSelector =
            new ComboBox<>();
    private final ComboBox<VersionRecord> versionSelector =
            new ComboBox<>();
    private final Label filePathLabel =
            new Label("No file selected");
    private final Label versionDetailsLabel =
            new Label("Select a file and version to view details.");
    private final Label statusLabel =
            new Label("Select a file to get started.");
    private final Button restoreButton =
            new Button("Restore Selected Version");
    public RestoreCenterView() {
        setStyle(
                "-fx-background-color: " + BACKGROUND + ";"
        );
        setPadding(new Insets(35));
        configureSelectors();
        VBox content = new VBox(22);
        content.getChildren().addAll(
                createHeader(),
                createRestoreCard(),
                createSafetyCard()
        );
        setCenter(content);
        fileSelector.valueProperty().addListener(
                (observable, oldFile, newFile) -> {

                    if (newFile == null) {
                        clearVersions(
                                "Select a file to view its versions."
                        );
                    } else {
                        loadVersions(newFile);
                    }
                }
        );
        versionSelector.valueProperty().addListener(
                (observable, oldVersion, newVersion) ->
                        updateVersionDetails(newVersion)
        );
        restoreButton.setOnAction(
                event -> showRestoreInformation()
        );
        loadFiles();
    }
    private VBox createHeader() {
        Label title = new Label("Restore Center");
        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );
        Label subtitle = new Label(
                "Inspect saved versions and prepare for file recovery."
        );
        subtitle.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: " + MUTED + ";"
        );
        return new VBox(6, title, subtitle);
    }
    private void configureSelectors() {
        fileSelector.setPromptText("Choose a tracked file");
        fileSelector.setMaxWidth(Double.MAX_VALUE);
        fileSelector.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(FileRecord file) {
                        return file == null
                                ? ""
                                : file.getFileName();
                    }
                    @Override
                    public FileRecord fromString(String value) {
                        return null;
                    }
                }
        );
        fileSelector.setCellFactory(
                list -> new ListCell<>() {
                    @Override
                    protected void updateItem(
                            FileRecord file,
                            boolean empty
                    ) {
                        super.updateItem(file, empty);
                        setText(
                                empty || file == null
                                        ? null
                                        : file.getFileName()
                        );
                        setTooltip(
                                empty || file == null
                                        ? null
                                        : new javafx.scene.control.Tooltip(
                                                file.getFilePath()
                                        )
                        );
                    }
                }
        );
        fileSelector.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(
                            FileRecord file,
                            boolean empty
                    ) {
                        super.updateItem(file, empty);
                        setText(
                                empty || file == null
                                        ? null
                                        : file.getFileName()
                        );
                    }
                }
        );
        versionSelector.setPromptText("Choose a version");
        versionSelector.setMaxWidth(Double.MAX_VALUE);
        versionSelector.setDisable(true);
        versionSelector.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(VersionRecord version) {
                        if (version == null) {
                            return "";
                        }

                        return "v"
                                + version.getVersionNumber()
                                + "  |  "
                                + safeText(version.getStorageType(), "UNKNOWN")
                                + "  |  "
                                + formatBytes(version.getFileSize());
                    }

                    @Override
                    public VersionRecord fromString(String value) {
                        return null;
                    }
                }
        );

        fileSelector.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8;"
        );

        versionSelector.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8;"
        );
    }
    private VBox createRestoreCard() {

        VBox card = new VBox(18);

        card.setPadding(new Insets(24));

        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 14;"
        );


        Label cardTitle = new Label(
                "Available Restore Points"
        );

        cardTitle.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        Label description = new Label(
                "Choose a tracked file and inspect its saved versions."
        );

        description.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        Label fileLabel = createFieldLabel("Tracked File");

        HBox fileRow = new HBox(18);

        fileRow.setAlignment(Pos.CENTER_LEFT);

        fileLabel.setPrefWidth(125);

        HBox.setHgrow(
                fileSelector,
                Priority.ALWAYS
        );

        fileRow.getChildren().addAll(
                fileLabel,
                fileSelector
        );


        filePathLabel.setWrapText(true);

        filePathLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        Label versionLabel = createFieldLabel(
                "Saved Version"
        );

        HBox versionRow = new HBox(18);

        versionRow.setAlignment(Pos.CENTER_LEFT);

        versionLabel.setPrefWidth(125);

        HBox.setHgrow(
                versionSelector,
                Priority.ALWAYS
        );

        versionRow.getChildren().addAll(
                versionLabel,
                versionSelector
        );


        Separator separator = new Separator();


        Label detailsTitle = new Label(
                "Version Details"
        );

        detailsTitle.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        versionDetailsLabel.setWrapText(true);

        versionDetailsLabel.setMinHeight(95);

        versionDetailsLabel.setMaxWidth(Double.MAX_VALUE);

        versionDetailsLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        VBox detailsBox = new VBox(12);

        detailsBox.setPadding(new Insets(18));

        detailsBox.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10;"
        );

        detailsBox.getChildren().addAll(
                detailsTitle,
                versionDetailsLabel
        );


        statusLabel.setWrapText(true);

        statusLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        configureRestoreButton();


        card.getChildren().addAll(
                cardTitle,
                description,
                fileRow,
                filePathLabel,
                versionRow,
                separator,
                detailsBox,
                statusLabel,
                restoreButton
        );

        return card;
    }


    private Label createFieldLabel(String text) {

        Label label = new Label(text);

        label.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        return label;
    }
    private void loadFiles() {
        try {
            Long userId =
                    userDao.findUserIdByUsername("local-user");

            if (userId == null) {
                userId = userDao.createUser("local-user");
            }
            List<FileRecord> files =
                    fileDao.findAllByUserId(userId);

            fileSelector.getItems().setAll(files);

            if (files.isEmpty()) {

                fileSelector.setDisable(true);

                filePathLabel.setText(
                        "No tracked files are registered."
                );

                clearVersions(
                        "No files are available yet. " +
                        "Register file records before selecting versions."
                );

                return;
            }
            fileSelector.setDisable(false);

            statusLabel.setText(
                    files.size()
                    + (files.size() == 1
                    ? " file loaded from SQLite."
                    : " files loaded from SQLite.")
            );

            fileSelector.getSelectionModel().selectFirst();

        } catch (SQLException e) {

            fileSelector.setDisable(true);

            clearVersions(
                    "Unable to load file records from the database."
            );

            statusLabel.setText(
                    "Database error: " + e.getMessage()
            );
        }
    }
    private void loadVersions(FileRecord file) {

        filePathLabel.setText(
                file.getFilePath()
        );

        try {

            List<VersionRecord> versions =
                    new ArrayList<>(
                            versionDao.findByFileId(file.getFileId())
                    );

            versions.sort(
                    Comparator.comparingInt(
                            VersionRecord::getVersionNumber
                    ).reversed()
            );

            versionSelector.getItems().setAll(versions);

            if (versions.isEmpty()) {

                clearVersionSelection();

                statusLabel.setText(
                        "No saved versions exist for this file yet."
                );

                return;
            }

            versionSelector.setDisable(false);

            statusLabel.setText(
                    versions.size()
                    + (versions.size() == 1
                    ? " version loaded from SQLite."
                    : " versions loaded from SQLite.")
            );

            versionSelector.getSelectionModel().selectFirst();

        } catch (SQLException e) {

            clearVersions(
                    "Unable to retrieve saved versions."
            );

            statusLabel.setText(
                    "Database error: " + e.getMessage()
            );
        }
    }
    private void updateVersionDetails(
            VersionRecord version
    ) {

        restoreButton.setDisable(version == null);

        if (version == null) {

            versionDetailsLabel.setText(
                    "No version selected."
            );

            return;
        }

        String details =
                "Version: v" + version.getVersionNumber()
                + "\nCreated: "
                + safeText(version.getCreatedAt(), "Unavailable")
                + "\nStorage type: "
                + safeText(version.getStorageType(), "Unknown")
                + "\nRecorded file size: "
                + formatBytes(version.getFileSize())
                + "\nSHA-256: "
                + safeText(version.getContentHash(), "Unavailable")
                + "\nStorage path: "
                + safeText(version.getStoragePath(), "Unavailable");
        versionDetailsLabel.setText(details);
    }
    private void clearVersions(String message) {

        versionSelector.getItems().clear();

        clearVersionSelection();

        filePathLabel.setText("No file selected");

        statusLabel.setText(message);
    }


    private void clearVersionSelection() {

        versionSelector.getSelectionModel().clearSelection();

        versionSelector.setDisable(true);

        versionDetailsLabel.setText(
                "No saved version selected."
        );

        restoreButton.setDisable(true);
    }
    private void configureRestoreButton() {

        restoreButton.setDisable(true);

        restoreButton.setPrefHeight(44);

        restoreButton.setMaxWidth(Double.MAX_VALUE);

        restoreButton.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                "-fx-background-radius: 9;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );

        restoreButton.setOnMouseEntered(event -> {

            if (!restoreButton.isDisabled()) {
                restoreButton.setStyle(
                        "-fx-background-color: #818CF8;" +
                        "-fx-background-radius: 9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                );
            }
        });

        restoreButton.setOnMouseExited(event ->
                restoreButton.setStyle(
                        "-fx-background-color: " + ACCENT + ";" +
                        "-fx-background-radius: 9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                )
        );

        restoreButton.setOnMousePressed(event -> {

            if (!restoreButton.isDisabled()) {

                ScaleTransition transition =
                        new ScaleTransition(
                                Duration.millis(80),
                                restoreButton
                        );

                transition.setToX(0.98);
                transition.setToY(0.98);
                transition.play();
            }
        });

        restoreButton.setOnMouseReleased(event -> {

            ScaleTransition transition =
                    new ScaleTransition(
                            Duration.millis(80),
                            restoreButton
                    );

            transition.setToX(1.0);
            transition.setToY(1.0);
            transition.play();
        });
    }


    private void showRestoreInformation() {

        FileRecord file =
                fileSelector.getValue();

        VersionRecord version =
                versionSelector.getValue();

        if (file == null || version == null) {
            return;
        }

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle("Restore Not Available Yet");

        alert.setHeaderText(
                "Version v" + version.getVersionNumber()
                + " is selected."
        );

        alert.setContentText(
                "File: " + file.getFileName()
                + "\n\nThe selected version was loaded from SQLite. "
                + "The actual restoration engine is planned for Phase 3. "
                + "No files have been changed."
        );

        alert.showAndWait();
    }
    private VBox createSafetyCard() {

        VBox card = new VBox(10);

        card.setPadding(new Insets(20));

        card.setStyle(
                "-fx-background-color: #17130A;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #3F3214;" +
                "-fx-border-radius: 12;"
        );


        Label title = new Label(
                "Restore Safety"
        );

        title.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + WARNING + ";"
        );


        Label description = new Label(
                "The planned restore workflow will preserve the current "
                + "file state before replacing it with an older version. "
                + "Actual file restoration is not enabled yet."
        );

        description.setWrapText(true);

        description.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        card.getChildren().addAll(
                title,
                description
        );

        return card;
    }
    private String safeText(
            String value,
            String fallback
    ) {

        return value == null || value.isBlank()
                ? fallback
                : value;
    }


    private String formatBytes(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        String[] units = {
                "KB", "MB", "GB", "TB"
        };

        double size = bytes;
        int unitIndex = -1;

        do {
            size /= 1024.0;
            unitIndex++;
        } while (
                size >= 1024.0
                && unitIndex < units.length - 1
        );

        return new DecimalFormat("0.##").format(size)
                + " " + units[unitIndex];
    }
}