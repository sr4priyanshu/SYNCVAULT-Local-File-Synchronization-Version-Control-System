package com.syncvault.ui;
import com.syncvault.db.dao.ActivityLogDao;
import com.syncvault.db.dao.DashboardStatsDao;
import com.syncvault.db.dao.UserDao;
import com.syncvault.db.models.ActivityLog;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
public class DashboardView extends BorderPane {
    private static final String BACKGROUND = "#0B1120";
    private static final String CARD = "#111827";
    private static final String BORDER = "#1F2937";
    private static final String TEXT = "#E5E7EB";
    private static final String MUTED = "#9CA3AF";
    private static final String GREEN = "#34D399";
    private final StatCard foldersCard;
    private final StatCard filesCard;
    private final StatCard versionsCard;
    private final StatCard versionDataCard;
    private final Label storageValueLabel;
    private final VBox activityList;
    public DashboardView() {
        setStyle(
                "-fx-background-color: " + BACKGROUND + ";"
        );
        setPadding(new Insets(35));
        VBox heading = createHeading();
        foldersCard = new StatCard(
                "Tracked Folders",
                "—",
                "Registered folders"
        );

        filesCard = new StatCard(
                "Total Files",
                "—",
                "Recorded file entries"
        );

        versionsCard = new StatCard(
                "Versions",
                "—",
                "Saved version records"
        );

        versionDataCard = new StatCard(
                "Version Data",
                "—",
                "Combined version sizes"
        );
        HBox statistics = createStatistics();
        storageValueLabel = new Label("Loading...");
        VBox storageCard = createStorageCard();
        activityList = new VBox(12);
        VBox activityCard = createActivityCard();
        HBox lowerSection = new HBox(
                18,
                storageCard,
                activityCard
        );
        HBox.setHgrow(
                storageCard,
                Priority.ALWAYS
        );
        HBox.setHgrow(
                activityCard,
                Priority.ALWAYS
        );
        VBox content = new VBox(
                25,
                heading,
                statistics,
                lowerSection
        );
        setCenter(content);
        loadDashboardData();
    }
    private VBox createHeading() {

        Label title = new Label("Dashboard");

        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );


        Label subtitle = new Label(
                "Monitor your files, versions and storage."
        );

        subtitle.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        return new VBox(
                6,
                title,
                subtitle
        );
    }

    private HBox createStatistics() {

        HBox cards = new HBox(18);

        cards.setAlignment(Pos.CENTER_LEFT);

        cards.getChildren().addAll(
                foldersCard,
                filesCard,
                versionsCard,
                versionDataCard
        );

        return cards;
    }
   private VBox createStorageCard() {

        VBox card = createCard();

        Label title = new Label(
                "Storage Overview"
        );

        title.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        Label description = new Label(
                "Recorded version data"
        );

        description.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        storageValueLabel.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );


        Label note = new Label(
                "Calculated from version metadata, " +
                "not physical disk usage."
        );

        note.setWrapText(true);

        note.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + MUTED + ";"
        );


        card.getChildren().addAll(
                title,
                description,
                storageValueLabel,
                note
        );

        return card;
    }
    private VBox createActivityCard() {

        VBox card = createCard();

        Label title = new Label(
                "Recent Activity"
        );

        title.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );


        ScrollPane scrollPane =
                new ScrollPane(activityList);

        scrollPane.setFitToWidth(true);

        scrollPane.setPrefHeight(175);

        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;"
        );


        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );


        card.getChildren().addAll(
                title,
                scrollPane
        );

        return card;
    }
    private String formatActivityTimestamp(String timestamp) {

    if (timestamp == null || timestamp.isBlank()) {
        return "Time unavailable";
    }

    try {

        DateTimeFormatter inputFormat =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm:ss"
                );

        DateTimeFormatter outputFormat =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm:ss a",
                        Locale.ENGLISH
                );

        LocalDateTime utcDateTime =
                LocalDateTime.parse(timestamp, inputFormat);

        return utcDateTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(outputFormat);

    } catch (DateTimeParseException e) {
        return timestamp;
    }
}
    private void displayActivityLogs(
            List<ActivityLog> logs
    ) {
        activityList.getChildren().clear();
        if (logs.isEmpty()) {
            Label emptyMessage = new Label(
                    "No activity recorded yet."
            );
            emptyMessage.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: " + MUTED + ";"
            );

            activityList.getChildren().add(
                    emptyMessage
            );

            return;
        }
        for (ActivityLog log : logs) {
            VBox entry = new VBox(5);
            String action = log.getAction();
            if (action == null || action.isBlank()) {
                action = "Activity";
            }
            Label actionLabel = new Label(
                    action.replace('_', ' ')
            );
            actionLabel.setStyle(
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + GREEN + ";"
            );
            String details = log.getDetails();
            if (details == null || details.isBlank()) {
                details = "No additional details";
            }
            Label detailsLabel = new Label(details);
            detailsLabel.setWrapText(true);
            detailsLabel.setStyle(
                    "-fx-font-size: 11px;" +
                    "-fx-text-fill: " + TEXT + ";"
            );
            String createdAt = log.getCreatedAt();
            createdAt =formatActivityTimestamp(log.getCreatedAt());
            Label dateLabel = new Label(createdAt);
            dateLabel.setStyle(
                    "-fx-font-size: 10px;" +
                    "-fx-text-fill: " + MUTED + ";"
            );
            entry.getChildren().addAll(
                    actionLabel,
                    detailsLabel,
                    dateLabel
            );
            entry.setPadding(
                    new Insets(10)
            );
            entry.setStyle(
                    "-fx-background-color: #0F172A;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 8;"
            );
            activityList.getChildren().add(entry);
        }
    }
    private void loadDashboardData() {
        try {
            UserDao userDao = new UserDao();
            Long userId =
                    userDao.findUserIdByUsername("local-user");
            if (userId == null) {

                userId = userDao.createUser("local-user");
            }
            DashboardStatsDao statsDao =
                    new DashboardStatsDao();
            DashboardStatsDao.DashboardStats stats =
                    statsDao.getStats(userId);
            foldersCard.setValue(
                    String.valueOf(stats.trackedFolders())
            );

            filesCard.setValue(
                    String.valueOf(stats.totalFiles())
            );

            versionsCard.setValue(
                    String.valueOf(stats.totalVersions())
            );


            String recordedSize =
                    formatBytes(stats.recordedVersionBytes());

            versionDataCard.setValue(recordedSize);

            storageValueLabel.setText(recordedSize);
            ActivityLogDao activityLogDao =
                    new ActivityLogDao();

            List<ActivityLog> logs =
                    activityLogDao.findRecentLogs(6);

            displayActivityLogs(logs);


        } catch (SQLException e) {

            foldersCard.setValue("—");
            filesCard.setValue("—");
            versionsCard.setValue("—");
            versionDataCard.setValue("—");

            storageValueLabel.setText(
                    "Unavailable"
            );

            displayDatabaseError();
        }
    }


    private void displayDatabaseError() {

        activityList.getChildren().clear();

        Label errorLabel = new Label(
                "Could not load dashboard data. " +
                "Check the database connection."
        );

        errorLabel.setWrapText(true);

        errorLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #FCA5A5;"
        );

        activityList.getChildren().add(errorLabel);
    }
    private String formatBytes(long bytes) {

        if (bytes < 0) {
            return "Unknown";
        }

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

        } while (size >= 1024.0
                && unitIndex < units.length - 1);


        return new DecimalFormat("0.##").format(size)
                + " " + units[unitIndex];
    }
    private VBox createCard() {

        VBox card = new VBox(12);

        card.setPadding(
                new Insets(22)
        );

        card.setPrefHeight(210);

        card.setMinWidth(0);

        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 14;"
        );

        addHoverAnimation(card);

        return card;
    }


    private void addHoverAnimation(VBox card) {

        card.setOnMouseEntered(event -> {

            ScaleTransition transition =
                    new ScaleTransition(
                            Duration.millis(120),
                            card
                    );

            transition.setToX(1.015);
            transition.setToY(1.015);

            transition.play();
        });


        card.setOnMouseExited(event -> {

            ScaleTransition transition =
                    new ScaleTransition(
                            Duration.millis(120),
                            card
                    );

            transition.setToX(1.0);
            transition.setToY(1.0);

            transition.play();
        });
    }
    private class StatCard extends VBox {
        private final Label valueLabel;


        StatCard(
                String title,
                String value,
                String description
        ) {

            super(8);

            setPadding(
                    new Insets(20)
            );

            setPrefWidth(190);

            setMinWidth(0);

            setPrefHeight(145);

            setMaxWidth(Double.MAX_VALUE);

            setAlignment(Pos.CENTER_LEFT);

            setStyle(
                    "-fx-background-color: " + CARD + ";" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 14;"
            );


            Label titleLabel = new Label(title);

            titleLabel.setWrapText(true);

            titleLabel.setStyle(
                    "-fx-font-size: 12px;" +
                    "-fx-text-fill: " + MUTED + ";"
            );


            valueLabel = new Label(value);

            valueLabel.setStyle(
                    "-fx-font-size: 27px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: white;"
            );


            Label descriptionLabel =
                    new Label(description);

            descriptionLabel.setWrapText(true);

            descriptionLabel.setStyle(
                    "-fx-font-size: 11px;" +
                    "-fx-text-fill: " + MUTED + ";"
            );


            getChildren().addAll(
                    titleLabel,
                    valueLabel,
                    descriptionLabel
            );

            addHoverAnimation(this);
        }


        void setValue(String value) {

            valueLabel.setText(value);
        }
    }
}