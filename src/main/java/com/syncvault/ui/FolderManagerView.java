package com.syncvault.ui;

import com.syncvault.db.dao.ActivityLogDao;
import com.syncvault.db.dao.FolderDao;
import com.syncvault.db.dao.UserDao;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class FolderManagerView extends BorderPane {

    private static final String BACKGROUND = "#0B1120";
    private static final String CARD = "#111827";
    private static final String BORDER = "#1F2937";
    private static final String TEXT = "#E5E7EB";
    private static final String MUTED = "#9CA3AF";
    private static final String ACCENT = "#6366F1";
    private static final String DANGER = "#EF4444";
    private static final String SUCCESS = "#34D399";

    private final VBox folderList;
    private final Label folderCountLabel;

    private final ActivityLogDao activityLogDao =
            new ActivityLogDao();

    private final FolderDao folderDao =
            new FolderDao();

    private final UserDao userDao =
            new UserDao();

    private long userId = -1;


    public FolderManagerView() {

        setStyle(
                "-fx-background-color: " + BACKGROUND + ";"
        );

        setPadding(new Insets(35));

        folderList = new VBox(12);
        folderCountLabel = new Label("0 folders");

        folderList.setPadding(new Insets(3));

        VBox content = new VBox(25);

        content.getChildren().addAll(
                createHeader(),
                createFolderCard()
        );

        setCenter(content);

        initializeFolders();
    }
    private HBox createHeader() {

        Label title = new Label("Folder Manager");

        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );

        Label subtitle = new Label(
                "Choose which folders SyncVault should protect."
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

        Button addButton =
                createPrimaryButton("+  Add Folder");

        addButton.setOnAction(
                event -> chooseFolder()
        );

        HBox header = new HBox();

        header.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(
                heading,
                Priority.ALWAYS
        );

        header.getChildren().addAll(
                heading,
                addButton
        );

        return header;
    }
    private VBox createFolderCard() {

        VBox card = new VBox(15);

        card.setPadding(new Insets(22));

        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 14;"
        );

        Label title = new Label("Tracked Folders");

        title.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        folderCountLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        HBox cardHeader = new HBox(10);

        cardHeader.setAlignment(Pos.CENTER_LEFT);

        cardHeader.getChildren().addAll(
                title,
                folderCountLabel
        );
        Separator separator = new Separator();
        separator.setStyle(
                "-fx-background-color: " + BORDER + ";"
        );
        ScrollPane scrollPane =
                new ScrollPane(folderList);

        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(430);

        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;"
        );

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );

        card.getChildren().addAll(
                cardHeader,
                separator,
                scrollPane
        );

        return card;
    }

    private void initializeFolders() {

        try {

            Long existingUserId =
                    userDao.findUserIdByUsername("local-user");

            if (existingUserId == null) {

                userId = userDao.createUser("local-user");

            } else {

                userId = existingUserId;
            }

            refreshFolderList();

        } catch (SQLException e) {

            showError(
                    "Unable to initialize the folder manager.",
                    e
            );

            showEmptyState();
        }
    }
    private void refreshFolderList()
            throws SQLException {

        List<String> paths =
                folderDao.findAllFolderPaths(userId);

        folderList.getChildren().clear();

        if (paths.isEmpty()) {

            showEmptyState();

        } else {

            for (String path : paths) {

                Long folderId =
                        folderDao.findFolderIdByPath(path);

                if (folderId != null) {

                    folderList.getChildren().add(
                            createFolderRow(path, folderId)
                    );
                }
            }
        }

        folderCountLabel.setText(
                paths.size()
                + (paths.size() == 1
                ? " folder"
                : " folders")
        );
    }
    private void showEmptyState() {

        Label icon = new Label("+");

        icon.setStyle(
                "-fx-font-size: 40px;" +
                "-fx-text-fill: " + ACCENT + ";"
        );

        Label title = new Label("No folders tracked");

        title.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label description = new Label(
                "Add a folder to start managing it in SyncVault."
        );

        description.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox emptyState = new VBox(8);

        emptyState.setAlignment(Pos.CENTER);

        emptyState.setPadding(new Insets(60));

        emptyState.getChildren().addAll(
                icon,
                title,
                description
        );

        folderList.getChildren().setAll(emptyState);

        folderCountLabel.setText("0 folders");
    }
    private void chooseFolder() {

        if (userId < 0) {

            showInfo(
                    "Database unavailable",
                    "The local application user could not be initialized."
            );

            return;
        }

        DirectoryChooser chooser =
                new DirectoryChooser();

        chooser.setTitle("Select Folder to Track");

        Window window = getScene() == null
                ? null
                : getScene().getWindow();

        File selectedFolder =
                chooser.showDialog(window);

        if (selectedFolder == null) {
            return;
        }

        String folderPath =
                selectedFolder.getAbsolutePath();

        try {

            // Prevent duplicate folder registration.
            if (folderDao.folderExists(folderPath)) {

                showInfo(
                        "Folder Already Exists",
                        "This folder is already registered in SyncVault."
                );

                return;
            }

            // Save the folder record in SQLite.
            folderDao.addFolder(
                    userId,
                    folderPath
            );

            // Log the successful folder addition.
            try {

                activityLogDao.addLog(
                        null,
                        "FOLDER_ADDED",
                        "Tracked folder: " + folderPath
                );

            } catch (SQLException logError) {

                System.err.println(
                        "Folder saved, but activity logging failed: "
                                + logError.getMessage()
                );
            }

            // Refresh the list from SQLite.
            refreshFolderList();

            showInfo(
                    "Folder Added",
                    "The folder has been saved to SyncVault's database."
            );

        } catch (SQLException e) {

            showError(
                    "Unable to save the selected folder.",
                    e
            );
        }
    }
    private HBox createFolderRow(
            String folderPath,
            long folderId
    ) {

        HBox row = new HBox(15);

        row.setAlignment(Pos.CENTER_LEFT);

        row.setPadding(new Insets(16));

        row.setStyle(
                "-fx-background-color: #0F172A;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10;"
        );

        Label folderIcon = new Label("▣");

        folderIcon.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-text-fill: " + ACCENT + ";"
        );

        VBox information = new VBox(5);

        Label path = new Label(folderPath);

        path.setMaxWidth(600);

        path.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        path.setTooltip(
                new Tooltip(folderPath)
        );

        Label status = new Label(
                "●  Saved in SQLite"
        );

        status.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + SUCCESS + ";"
        );

        information.getChildren().addAll(
                path,
                status
        );

        Button removeButton =
                createDangerButton("Remove");

        removeButton.setOnAction(
                event -> removeFolder(folderId, folderPath)
        );

        HBox.setHgrow(
                information,
                Priority.ALWAYS
        );

        row.getChildren().addAll(
                folderIcon,
                information,
                removeButton
        );

        addRowAnimation(row);

        return row;
    }
    private void removeFolder(
            long folderId,
            String folderPath
    ) {
        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Tracked Folder");
        confirmation.setHeaderText(
                "Remove this folder from SyncVault?"
        );
        confirmation.setContentText(
                folderPath +
                "\n\nThis removes the folder record and any linked " +
                "file/version metadata from SQLite. The original " +
                "folder and its files on your computer will not be deleted."
        );
        Optional<ButtonType> result =
                confirmation.showAndWait();
        if (result.isEmpty()
                || result.get() != ButtonType.OK) {
            return;
        }
        try {
            boolean removed =
                    folderDao.removeFolder(folderId);
            if (removed) {
                try {

                    activityLogDao.addLog(
                            null,
                            "FOLDER_REMOVED",
                            "Removed tracked folder: " + folderPath
                    );

                } catch (SQLException logError) {

                    System.err.println(
                            "Folder removed, but activity logging failed: "
                                    + logError.getMessage()
                    );
                }
                refreshFolderList();
                showInfo(
                        "Folder Removed",
                        "The folder record was removed from SyncVault."
                );
            } else {
                showInfo(
                        "Folder Not Found",
                        "The folder record no longer exists."
                );
            }
        } catch (SQLException e) {

            showError(
                    "Unable to remove the folder record.",
                    e
            );
        }
    }
    private Button createPrimaryButton(String text) {

        Button button = new Button(text);

        button.setPrefHeight(42);

        button.setPadding(
                new Insets(0, 20, 0, 20)
        );

        button.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                "-fx-background-radius: 9;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(
                event -> button.setStyle(
                        "-fx-background-color: #818CF8;" +
                        "-fx-background-radius: 9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                )
        );

        button.setOnMouseExited(
                event -> button.setStyle(
                        "-fx-background-color: " + ACCENT + ";" +
                        "-fx-background-radius: 9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                )
        );

        addButtonAnimation(button);

        return button;
    }
    private Button createDangerButton(String text) {

        Button button = new Button(text);

        button.setPrefHeight(34);

        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: #374151;" +
                "-fx-border-radius: 7;" +
                "-fx-background-radius: 7;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 11px;" +
                "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(
                event -> button.setStyle(
                        "-fx-background-color: #3F1D1D;" +
                        "-fx-border-color: " + DANGER + ";" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;" +
                        "-fx-text-fill: #FCA5A5;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;"
                )
        );

        button.setOnMouseExited(
                event -> button.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: #374151;" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;" +
                        "-fx-text-fill: " + MUTED + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;"
                )
        );

        addButtonAnimation(button);

        return button;
    }
    private void addButtonAnimation(Button button) {

        button.setOnMousePressed(event -> {

            ScaleTransition transition =
                    new ScaleTransition(
                            Duration.millis(80),
                            button
                    );

            transition.setToX(0.96);
            transition.setToY(0.96);

            transition.play();
        });

        button.setOnMouseReleased(event -> {

            ScaleTransition transition =
                    new ScaleTransition(
                            Duration.millis(80),
                            button
                    );

            transition.setToX(1.0);
            transition.setToY(1.0);

            transition.play();
        });
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
                        "-fx-background-color: #0F172A;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 10;"
                )
        );
    }
    private void showInfo(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
   private void showError(
            String message,
            SQLException exception
    ) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle("SyncVault Database Error");

        alert.setHeaderText(message);

        alert.setContentText(
                exception.getMessage()
        );

        alert.showAndWait();
    }
}