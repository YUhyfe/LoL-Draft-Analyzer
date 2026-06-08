package com.yuhyfe.loldraftanalyzer;

import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        stage.setTitle("LoL Draft Analyzer");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
