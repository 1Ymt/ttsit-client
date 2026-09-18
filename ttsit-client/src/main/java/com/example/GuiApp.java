package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/gui/main.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/example/gui/theme.css").toExternalForm());

        stage.setTitle("TTS Client");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        stage.show();
    }
}
