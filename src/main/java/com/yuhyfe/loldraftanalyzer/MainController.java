package com.yuhyfe.loldraftanalyzer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Kontroler shellu — odpowiada za routing między widokami.
 * Każdy widok (meta / profile / champions / mecze / rankingi) jest osobnym FXML,
 * który ten kontroler ładuje do StackPane content-area po kliknięciu w tab.
 */
public class MainController {

    // ---- Widoki załadowane na starcie (cache) ----
    private Parent profilePage;
    private Parent championsPage;
    private Parent metaPage;
    private Parent meczePage;
    private Parent rankingiPage;

    // ---- Aktualnie aktywny tab ----
    private Button activeTab;

    // ---- @FXML injections z view.fxml ----
    @FXML private StackPane contentArea;
    @FXML private Button navProfil;
    @FXML private Button navBohaterowie;
    @FXML private Button navMeta;
    @FXML private Button navMecze;
    @FXML private Button navRankingi;

    /**
     * Wywoływane automatycznie przez FXMLLoader po wstrzyknięciu pól @FXML.
     */
    @FXML
    public void initialize() {
        // Ładujemy wszystkie strony raz na starcie — szybsze przełączanie później.
        try {
            profilePage   = loadPage("profile.fxml");
            championsPage = loadPage("champions.fxml");
            metaPage      = loadPage("meta.fxml");
            meczePage     = loadPage("mecze.fxml");
            rankingiPage  = loadPage("rankingi.fxml");
        } catch (IOException e) {
            System.err.println("Błąd ładowania widoku: " + e.getMessage());
            e.printStackTrace();
        }

        // Wire-up klików w taby.
        navProfil.setOnAction(e -> switchTo(profilePage, navProfil));
        navBohaterowie.setOnAction(e -> switchTo(championsPage, navBohaterowie));
        navMeta.setOnAction(e -> switchTo(metaPage, navMeta));
        navMecze.setOnAction(e -> switchTo(meczePage, navMecze));
        navRankingi.setOnAction(e -> switchTo(rankingiPage, navRankingi));

        // Domyślny widok przy starcie — Profil (jak active w FXML).
        // activeTab jest jeszcze null → setActive wykryje to i nie próbuje czyścić.
        switchTo(profilePage, navProfil);
    }

    private Parent loadPage(String fxmlName) throws IOException {
        URL url = getClass().getResource(fxmlName);
        if (url == null) {
            throw new IOException("Nie znaleziono pliku FXML: " + fxmlName);
        }
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
        Label lbl = new Label("Strona „" + name + "” — w budowie");
        lbl.getStyleClass().addAll("text-muted", "text-lg");
        contentArea.getChildren().setAll(lbl);
        setActive(tab);
    }

    /**
     * Aktualizuje wizualnie który tab jest podświetlony (klasa CSS "active").
     */
    private void setActive(Button newActive) {
        if (activeTab != null) {
            activeTab.getStyleClass().remove("active");
        }
        if (!newActive.getStyleClass().contains("active")) {
            newActive.getStyleClass().add("active");
        }
        activeTab = newActive;
    }
}
