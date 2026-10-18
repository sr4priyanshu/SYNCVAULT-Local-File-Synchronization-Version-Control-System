package com.syncvault;
import com.syncvault.db.DatabaseInitializer;
import com.syncvault.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class App extends Application {
    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();
        MainView mainView = new MainView();
        Scene scene = new Scene(
                mainView,
                1100,
                700
        );
        stage.setTitle("SyncVault");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}