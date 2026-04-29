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
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        LcuConnector connector = new LcuConnector();

        int i = 0;
        for (String string : connector.readLockFile()) {
            System.out.println("Id: " + i + " - " + string);
            i++;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}