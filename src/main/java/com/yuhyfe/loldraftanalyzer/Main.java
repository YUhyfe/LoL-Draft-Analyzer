package com.yuhyfe.loldraftanalyzer;

import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("profile.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setTitle("LoL Draft Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}