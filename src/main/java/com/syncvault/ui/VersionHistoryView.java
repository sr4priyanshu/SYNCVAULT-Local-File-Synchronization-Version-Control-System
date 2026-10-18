package com.syncvault.ui;

import com.syncvault.db.dao.FileDao;
import com.syncvault.db.dao.UserDao;
import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.FileRecord;
import com.syncvault.db.models.VersionRecord;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class VersionHistoryView extends BorderPane {

    private static final String BACKGROUND = "#0B1120";
    private static final String CARD = "#111827";
    private static final String ROW = "#0F172A";
    private static final String BORDER = "#1F2937";
    private static final String TEXT = "#E5E7EB";
    private static final String MUTED = "#9CA3AF";
    private static final String ACCENT = "#6366F1";
    private static final String GREEN = "#34D399";
    private static final String BLUE = "#60A5FA";

    private final FileDao fileDao = new FileDao();
    private final UserDao userDao = new UserDao();
    private final VersionDao versionDao = new VersionDao();

    private final ComboBox<FileRecord> fileSelector =
            new ComboBox<>();

    private final Label selectedFilePathLabel =
            new Label("Select a file");

    private final Label versionCountLabel =
            new Label("0 versions");

    private final VBox versionList = new VBox(10);


    public VersionHistoryView() {

        setStyle(
                "-fx-background-color: " + BACKGROUND + ";"
        );

        setPadding(new Insets(35));

        VBox content = new VBox(25);

        HBox header = createHeader();

        VBox historyCard = createHistoryCard();

        content.getChildren().addAll(
                header,
                historyCard
        );

        setCenter(content);

        // Selection changes load the corresponding file history.
        fileSelector.valueProperty().addListener(
                (observable, oldFile, newFile) -> {

                    if (newFile == null) {
                        showEmptyState(
                                "Select a file to view its versions."
                        );
                    } else {
                        loadVersions(newFile);
                    }
                }
        );

        loadFiles();
    }



    private HBox createHeader() {

        Label title = new Label("Version History");

        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );

        Label subtitle = new Label(
                "Browse saved versions of your tracked files."
        );

        subtitle.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox heading = new VBox(6);

        heading.getChildren().addAll(
                title,
                subtitle
        );

        HBox header = new HBox();

        header.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().add(heading);

        return header;
    }


    private VBox createHistoryCard() {

        VBox card = new VBox(15);

        card.setPadding(new Insets(22));

        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 14;"
        );

        Label selectorTitle = new Label(
                "Select File"
        );

        selectorTitle.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        fileSelector.setPromptText("Choose a tracked file");
        fileSelector.setPrefWidth(450);
        fileSelector.setMaxWidth(Double.MAX_VALUE);

        fileSelector.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8;" +
                "-fx-text-fill: " + TEXT + ";"
        );

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
                listView -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            FileRecord file,
                            boolean empty
                    ) {

                        super.updateItem(file, empty);

                        if (empty || file == null) {
                            setText(null);
                        } else {
                            setText(file.getFileName());
                        }
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


        selectedFilePathLabel.setWrapText(true);

        selectedFilePathLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        VBox fileHeader = new VBox(10);

        fileHeader.getChildren().addAll(
                selectorTitle,
                fileSelector,
                selectedFilePathLabel
        );


        HBox historyHeading = new HBox(12);

historyHeading.setAlignment(Pos.CENTER_LEFT);

Label historyTitle = new Label("Saved Versions");

historyTitle.setStyle(
        "-fx-font-size: 17px;" +
        "-fx-font-weight: bold;" +
        "-fx-text-fill: " + TEXT + ";"
);


javafx.scene.layout.Region spacer =
        new javafx.scene.layout.Region();

HBox.setHgrow(spacer, Priority.ALWAYS);

versionCountLabel.setStyle(
        "-fx-background-color: #1F2937;" +
        "-fx-background-radius: 7;" +
        "-fx-padding: 6 10;" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;" +
        "-fx-text-fill: " + MUTED + ";"
);

historyHeading.getChildren().addAll(
        historyTitle,
        spacer,
        versionCountLabel
);

        Separator separator = new Separator();

        separator.setStyle(
                "-fx-background-color: " + BORDER + ";"
        );


        ScrollPane scrollPane = new ScrollPane(versionList);

        scrollPane.setFitToWidth(true);

        scrollPane.setPrefHeight(400);

        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;"
        );

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );

        card.getChildren().addAll(
                fileHeader,
                separator,
                historyHeading,
                scrollPane
        );

        return card;
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

                selectedFilePathLabel.setText(
                        "No tracked files are registered yet."
                );

                showEmptyState(
                        "No files found. Add a tracked folder and " +
                        "register file records to view version history."
                );

            } else {

                fileSelector.setDisable(false);

                // Selecting the first file loads its versions.
                fileSelector.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {

            fileSelector.setDisable(true);

            selectedFilePathLabel.setText(
                    "Unable to retrieve files from SQLite."
            );

            showEmptyState(
                    "Database error: " + e.getMessage()
            );
        }
    }


    private void loadVersions(FileRecord file) {

        selectedFilePathLabel.setText(
                file.getFilePath()
        );

        try {

            List<VersionRecord> versions =
                    versionDao.findByFileId(file.getFileId());

            // Display newest versions first.
            versions.sort(
                    Comparator.comparingInt(
                            VersionRecord::getVersionNumber
                    ).reversed()
            );

            versionList.getChildren().clear();

            versionCountLabel.setText(
                    versions.size()
                    + (versions.size() == 1
                    ? " version"
                    : " versions")
            );


            if (versions.isEmpty()) {

                showEmptyState(
                        "No versions have been saved for this file yet."
                );

                return;
            }


            for (int i = 0; i < versions.size(); i++) {

                VersionRecord version = versions.get(i);

                boolean latest = (i == 0);

                versionList.getChildren().add(
                        createVersionRow(version, latest)
                );
            }

        } catch (SQLException e) {

            showEmptyState(
                    "Unable to load version history: "
                    + e.getMessage()
            );
        }
    }

    private HBox createVersionRow(
            VersionRecord version,
            boolean latest
    ) {

        HBox row = new HBox(18);

        row.setAlignment(Pos.CENTER_LEFT);

        row.setPadding(new Insets(15));

        row.setStyle(
                "-fx-background-color: " + ROW + ";" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10;"
        );


        Label versionLabel = new Label(
                "v" + version.getVersionNumber()
        );

        versionLabel.setPrefWidth(48);

        versionLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + ACCENT + ";"
        );


        VBox information = new VBox(5);

        Label descriptionLabel = new Label(
                latest
                        ? "Latest saved version"
                        : "Saved version"
        );

        descriptionLabel.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        String createdAt = version.getCreatedAt();

        if (createdAt == null || createdAt.isBlank()) {
            createdAt = "Date unavailable";
        }

        Label dateLabel = new Label(createdAt);

        dateLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        information.getChildren().addAll(
                descriptionLabel,
                dateLabel
        );


        String storageType = version.getStorageType();

        if (storageType == null || storageType.isBlank()) {
            storageType = "UNKNOWN";
        }

        storageType = storageType.toUpperCase();


        Label typeLabel = new Label(storageType);

        boolean isDelta = storageType.equals("DELTA");

        typeLabel.setStyle(
                "-fx-background-color: " +
                (isDelta ? "#172554" : "#1E293B") + ";" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 5 9;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " +
                (isDelta ? BLUE : MUTED) + ";"
        );


        Label sizeLabel = new Label(
                formatBytes(version.getFileSize())
        );

        sizeLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        HBox.setHgrow(
                information,
                Priority.ALWAYS
        );


        row.getChildren().addAll(
                versionLabel,
                information,
                typeLabel,
                sizeLabel
        );


        if (latest) {

            Label currentLabel = new Label("CURRENT");

            currentLabel.setStyle(
                    "-fx-background-color: #064E3B;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 5 8;" +
                    "-fx-font-size: 9px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + GREEN + ";"
            );

            row.getChildren().add(currentLabel);
        }


        addRowAnimation(row);

        return row;
    }


    private void showEmptyState(String message) {

        versionList.getChildren().clear();

        Label icon = new Label("◷");

        icon.setStyle(
                "-fx-font-size: 36px;" +
                "-fx-text-fill: " + ACCENT + ";"
        );
        Label messageLabel = new Label(message);

        messageLabel.setWrapText(true);

        messageLabel.setMaxWidth(500);

        messageLabel.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + MUTED + ";"
        );
        VBox emptyState = new VBox(10);

        emptyState.setAlignment(Pos.CENTER);

        emptyState.setPadding(new Insets(45));

        emptyState.getChildren().addAll(
                icon,
                messageLabel
        );

        versionList.getChildren().add(emptyState);

        versionCountLabel.setText("0 versions");
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

    private void addRowAnimation(HBox row) {

        row.setOnMouseEntered(
                event -> row.setStyle(
                        "-fx-background-color: #172033;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + ACCENT + ";" +
                        "-fx-border-radius: 10;"
                )
        );
        row.setOnMouseExited(
                event -> row.setStyle(
                        "-fx-background-color: " + ROW + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 10;"
                )
        );
    }
}