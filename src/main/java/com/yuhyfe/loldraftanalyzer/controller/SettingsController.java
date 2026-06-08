package com.yuhyfe.loldraftanalyzer.controller;

import com.yuhyfe.loldraftanalyzer.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class SettingsController {

    @FXML private PasswordField    apiKeyField;
    @FXML private TextField        apiKeyVisible;
    @FXML private ComboBox<String> regionCombo;
    @FXML private Label            statusLabel;
    @FXML private TextField        lolPathField;

    private boolean showingKey = false;

    @FXML
    public void initialize() {
        AppSettings s = AppSettings.get();
        apiKeyField.setText(s.getRiotApiKey());
        apiKeyVisible.setText(s.getRiotApiKey());
        apiKeyVisible.setVisible(false);
        apiKeyVisible.setManaged(false);

        regionCombo.getItems().addAll(
            "EUW", "EUNE", "NA", "KR", "JP", "BR", "LAN", "LAS", "TR", "RU", "OCE"
        );
        String current = s.getRegion();
        if (!current.isBlank() && regionCombo.getItems().contains(current)) {
            regionCombo.setValue(current);
        } else {
            regionCombo.setValue("EUW");
        }

        lolPathField.setText(s.getLolPath());

        apiKeyField.textProperty().addListener((obs, o, n) -> {
            if (!showingKey) apiKeyVisible.setText(n);
        });
        apiKeyVisible.textProperty().addListener((obs, o, n) -> {
            if (showingKey) apiKeyField.setText(n);
        });
    }

    @FXML
    private void handleToggleVisibility() {
        showingKey = !showingKey;
        apiKeyField.setVisible(!showingKey);
        apiKeyField.setManaged(!showingKey);
        apiKeyVisible.setVisible(showingKey);
        apiKeyVisible.setManaged(showingKey);
    }

    @FXML
    private void handleSave() {
        String key    = apiKeyField.getText().strip();
        String region = regionCombo.getValue();
        if (region == null || region.isBlank()) {
            statusLabel.setText("Wybierz region.");
            statusLabel.setStyle("-fx-text-fill: -hex-danger;");
            return;
        }
        AppSettings settings = AppSettings.get();
        settings.setRiotApiKey(key);
        settings.setRegion(region);
        settings.setLolPath(lolPathField.getText().strip());
        com.yuhyfe.loldraftanalyzer.lcu.LcuConnector.reset();
        settings.save();
        closeWindow();
    }

    @FXML
    private void handleBrowseLolPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Wybierz folder League of Legends");
        String current = lolPathField.getText().strip();
        if (!current.isBlank()) {
            File init = new File(current);
            if (init.exists()) chooser.setInitialDirectory(init);
        }
        Stage stage = (Stage) lolPathField.getScene().getWindow();
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            lolPathField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) apiKeyField.getScene().getWindow();
        stage.close();
    }
}
