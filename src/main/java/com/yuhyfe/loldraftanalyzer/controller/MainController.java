package com.yuhyfe.loldraftanalyzer.controller;

import com.yuhyfe.loldraftanalyzer.util.AppLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOG = AppLogger.get(MainController.class);

    private Parent profilePage;
    private Parent championsPage;
    private Parent metaPage;
    private Parent meczePage;
    private Parent rankingiPage;

    private Button activeTab;

    @FXML private StackPane contentArea;
    @FXML private Button navProfil;
    @FXML private Button navBohaterowie;
    @FXML private Button navMeta;
    @FXML private Button navMecze;
    @FXML private Button navRankingi;
    @FXML private Button btnSettings;

    @FXML
    public void initialize() {
        try {
            profilePage   = loadPage("profile.fxml");
            championsPage = loadPage("champions.fxml");
            metaPage      = loadPage("meta.fxml");
            meczePage     = loadPage("mecze.fxml");
            rankingiPage  = loadPage("rankingi.fxml");
        } catch (IOException e) {
            LOG.severe("Błąd ładowania widoku: " + e.getMessage());
        }

        navProfil.setOnAction(e -> switchTo(profilePage, navProfil));
        navBohaterowie.setOnAction(e -> switchTo(championsPage, navBohaterowie));
        navMeta.setOnAction(e -> switchTo(metaPage, navMeta));
        navMecze.setOnAction(e -> switchTo(meczePage, navMecze));
        navRankingi.setOnAction(e -> switchTo(rankingiPage, navRankingi));

        if (btnSettings != null) {
            btnSettings.setOnAction(e -> openSettings());
        }

        switchTo(profilePage, navProfil);
    }

    private void openSettings() {
        try {
            URL url = getClass().getResource("/com/yuhyfe/loldraftanalyzer/settings.fxml");
            if (url == null) return;
            Parent root = FXMLLoader.load(url);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(contentArea.getScene().getWindow());
            dialog.setTitle("Ustawienia");
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            LOG.warning("Nie można otworzyć ustawień: " + e.getMessage());
        }
    }

    private Parent loadPage(String fxmlName) throws IOException {
        URL url = getClass().getResource("/com/yuhyfe/loldraftanalyzer/" + fxmlName);
        if (url == null) throw new IOException("Nie znaleziono pliku FXML: " + fxmlName);
        return FXMLLoader.load(url);
    }

    private void switchTo(Parent page, Button tab) {
        if (page == null) {
            showPlaceholder(tab.getText() + " (błąd ładowania)", tab);
            return;
        }
        contentArea.getChildren().setAll(page);
        setActive(tab);
    }

    private void showPlaceholder(String name, Button tab) {
        Label lbl = new Label("Strona \"" + name + "\" - w budowie");
        lbl.getStyleClass().addAll("text-muted", "text-lg");
        contentArea.getChildren().setAll(lbl);
        setActive(tab);
    }

    private void setActive(Button newActive) {
        if (activeTab != null) activeTab.getStyleClass().remove("active");
        if (!newActive.getStyleClass().contains("active")) newActive.getStyleClass().add("active");
        activeTab = newActive;
    }
}
