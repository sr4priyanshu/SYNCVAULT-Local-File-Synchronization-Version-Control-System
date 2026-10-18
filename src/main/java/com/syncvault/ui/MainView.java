package com.syncvault.ui;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainView extends BorderPane {

    private final VBox contentArea;

    private final Button dashboardButton;
    private final Button foldersButton;
    private final Button versionsButton;
    private final Button restoreButton;

    private static final String SIDEBAR_COLOR = "#111827";
    private static final String BACKGROUND_COLOR = "#0B1120";
    private static final String TEXT_COLOR = "#E5E7EB";
    private static final String SECONDARY_TEXT = "#9CA3AF";
    private static final String ACCENT_COLOR = "#6366F1";
    private static final String HOVER_COLOR = "#1F2937";


    public MainView() {

        setStyle(
                "-fx-background-color: " +
                BACKGROUND_COLOR + ";"
        );


        contentArea = new VBox();

        contentArea.setFillWidth(true);

        contentArea.setStyle(
                "-fx-background-color: " +
                BACKGROUND_COLOR + ";"
        );


        dashboardButton =
                createNavigationButton("Dashboard");

        foldersButton =
                createNavigationButton("Folders");

        versionsButton =
                createNavigationButton("Version History");

        restoreButton =
                createNavigationButton("Restore Center");


        VBox sidebar = createSidebar();


        setLeft(sidebar);
        setCenter(contentArea);


        showDashboard();

        setActiveButton(
                dashboardButton
        );
    }


    private VBox createSidebar() {

        Label logo =
                new Label("SyncVault");

        logo.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
        );


        Label subtitle =
                new Label("FILE VERSION CONTROL");

        subtitle.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " +
                SECONDARY_TEXT + ";" +
                "-fx-letter-spacing: 1.5px;"
        );


        VBox branding =
                new VBox(5);

        branding.getChildren().addAll(
                logo,
                subtitle
        );


        VBox navigation =
                new VBox(8);

        navigation.getChildren().addAll(
                dashboardButton,
                foldersButton,
                versionsButton,
                restoreButton
        );


        Label status =
                new Label("●  SYSTEM READY");

        status.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34D399;"
        );


        VBox sidebar =
                new VBox(35);

        sidebar.setPadding(
                new Insets(
                        30,
                        18,
                        25,
                        18
                )
        );

        sidebar.setPrefWidth(240);

        sidebar.setMinWidth(240);

        sidebar.setStyle(
                "-fx-background-color: " +
                SIDEBAR_COLOR + ";"
        );


        VBox spacer = new VBox();

        VBox.setVgrow(
                spacer,
                javafx.scene.layout.Priority.ALWAYS
        );


        sidebar.getChildren().addAll(
                branding,
                navigation,
                spacer,
                status
        );


        setupNavigation();


        return sidebar;
    }


    private Button createNavigationButton(
            String text
    ) {

        Button button =
                new Button(text);


        button.setMaxWidth(
                Double.MAX_VALUE
        );


        button.setPrefHeight(48);


        button.setAlignment(
                Pos.CENTER_LEFT
        );


        button.setPadding(
                new Insets(
                        0,
                        18,
                        0,
                        18
                )
        );


        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: " +
                SECONDARY_TEXT + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );


        button.setOnMouseEntered(event -> {

            if (!button.getStyle().contains(
                    ACCENT_COLOR
            )) {

                button.setStyle(
                        "-fx-background-color: " +
                        HOVER_COLOR + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-text-fill: " +
                        TEXT_COLOR + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                );
            }
        });


        button.setOnMouseExited(event -> {

            if (!button.getStyle().contains(
                    ACCENT_COLOR
            )) {

                button.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 10;" +
                        "-fx-text-fill: " +
                        SECONDARY_TEXT + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
                );
            }
        });


        addButtonAnimation(button);


        return button;
    }


    private void addButtonAnimation(
            Button button
    ) {

        button.setOnMousePressed(event -> {

            ScaleTransition animation =
                    new ScaleTransition(
                            Duration.millis(80),
                            button
                    );

            animation.setToX(0.96);
            animation.setToY(0.96);

            animation.play();
        });


        button.setOnMouseReleased(event -> {

            ScaleTransition animation =
                    new ScaleTransition(
                            Duration.millis(80),
                            button
                    );

            animation.setToX(1.0);
            animation.setToY(1.0);

            animation.play();
        });
    }


    private void setupNavigation() {

        dashboardButton.setOnAction(event -> {

            setActiveButton(
                    dashboardButton
            );

            showDashboard();
        });


        foldersButton.setOnAction(event -> {

            setActiveButton(
                    foldersButton
            );

            showFolders();
        });


        versionsButton.setOnAction(event -> {

            setActiveButton(
                    versionsButton
            );

            showVersions();
        });


        restoreButton.setOnAction(event -> {

            setActiveButton(
                    restoreButton
            );

            showRestore();
        });
    }


    private void setActiveButton(
            Button activeButton
    ) {

        Button[] buttons = {
                dashboardButton,
                foldersButton,
                versionsButton,
                restoreButton
        };


        for (Button button : buttons) {

            button.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-background-radius: 10;" +
                    "-fx-text-fill: " +
                    SECONDARY_TEXT + ";" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
            );
        }


        activeButton.setStyle(
                "-fx-background-color: " +
                ACCENT_COLOR + ";" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
    }


    private void showDashboard() {

        DashboardView dashboardView =
                new DashboardView();

        displayView(dashboardView);
    }


    private void showFolders() {

        FolderManagerView folderManagerView =
                new FolderManagerView();

        displayView(folderManagerView);
    }


    private void showVersions() {

        VersionHistoryView versionHistoryView =
                new VersionHistoryView();

        displayView(versionHistoryView);
    }


    private void showRestore() {

        RestoreCenterView restoreCenterView =
                new RestoreCenterView();

        displayView(restoreCenterView);
    }


    private void displayView(
            javafx.scene.Node view
    ) {

        contentArea.getChildren().setAll(view);

        VBox.setVgrow(
                view,
                javafx.scene.layout.Priority.ALWAYS
        );
    }


    public void setContent(
            javafx.scene.Node node
    ) {

        displayView(node);
    }
}