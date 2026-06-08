package com.yuhyfe.loldraftanalyzer.controller;

import com.yuhyfe.loldraftanalyzer.model.champion.PersonalChampionStats;
import com.yuhyfe.loldraftanalyzer.service.ChampionStatsService;
import com.yuhyfe.loldraftanalyzer.service.DataDragonService;
import com.yuhyfe.loldraftanalyzer.service.SummonerService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ChampionsController {

    @FXML private TextField searchField;
    @FXML private Button filterAll;
    @FXML private Button filterTop;
    @FXML private Button filterJungle;
    @FXML private Button filterMid;
    @FXML private Button filterAdc;
    @FXML private Button filterSupport;
    @FXML private ComboBox<String> sortCombo;

    @FXML private FlowPane champGrid;

    @FXML private Label kpiPlayed;
    @FXML private Label kpiTopChamp;
    @FXML private Label kpiTopRole;
    @FXML private Label kpiOverallWr;

    @FXML private Label statusLabel;

    private final DataDragonService    dataDragonService   = new DataDragonService();
    private final SummonerService      summonerService     = new SummonerService();
    private final ChampionStatsService champStatsService   = new ChampionStatsService(dataDragonService);

    private List<PersonalChampionStats> allStats;
    private String currentRole = "ALL";
    private String currentSort = "Gry (malejąco)";

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, o, n) -> rebuildGrid());

        filterAll.setOnAction(e     -> setRole("ALL",     filterAll));
        filterTop.setOnAction(e     -> setRole("TOP",     filterTop));
        filterJungle.setOnAction(e  -> setRole("JUNGLE",  filterJungle));
        filterMid.setOnAction(e     -> setRole("MID",     filterMid));
        filterAdc.setOnAction(e     -> setRole("ADC",     filterAdc));
        filterSupport.setOnAction(e -> setRole("SUPPORT", filterSupport));

        sortCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) { currentSort = n; rebuildGrid(); }
        });

        loadStats();
    }

    @FXML
    private void handleRefresh() {
        allStats = null;
        loadStats();
    }

    @FXML private void handleFilterAll() { setRole("ALL", filterAll); }

    private void loadStats() {
        setStatus("Ładowanie historii meczów…");
        champGrid.getChildren().clear();
        Label loading = new Label("Ładowanie…");
        loading.getStyleClass().addAll("text-muted", "text-lg");
        champGrid.getChildren().add(loading);

        Task<List<PersonalChampionStats>> task = new Task<>() {
            @Override
            protected List<PersonalChampionStats> call() throws Exception {
                com.yuhyfe.loldraftanalyzer.model.summoner.Summoner s = summonerService.getCurrentSummoner();
                return champStatsService.getChampionStats(s.getPuuid(), s.getGameName());
            }
        };

        task.setOnSucceeded(e -> {
            allStats = task.getValue();
            updateKpi(allStats);
            rebuildGrid();
            setStatus("Załadowano " + allStats.size() + " postaci z ostatnich 100 meczów");
        });

        task.setOnFailed(e -> {
            champGrid.getChildren().clear();
            Label err = new Label("Nie udało się załadować danych — sprawdź czy klient League jest uruchomiony");
            err.getStyleClass().addAll("text-muted", "text-lg");
            champGrid.getChildren().add(err);
            setStatus("Błąd połączenia z LCU");
        });

        daemon(task);
    }

    private void setRole(String role, Button btn) {
        currentRole = role;
        for (Button b : new Button[]{filterAll, filterTop, filterJungle, filterMid, filterAdc, filterSupport}) {
            b.getStyleClass().remove("active");
        }
        btn.getStyleClass().add("active");
        rebuildGrid();
    }

    private void rebuildGrid() {
        if (allStats == null) return;

        String search = searchField.getText().trim().toLowerCase();

        List<PersonalChampionStats> visible = allStats.stream()
                .filter(s -> "ALL".equals(currentRole) || currentRole.equals(s.getLaneFilterRole()))
                .filter(s -> search.isEmpty() || s.getChampionName().toLowerCase().contains(search))
                .collect(Collectors.toList());

        visible = sorted(visible);

        champGrid.getChildren().clear();
        if (visible.isEmpty()) {
            Label empty = new Label("Brak postaci spełniających kryteria");
            empty.getStyleClass().addAll("text-muted", "text-lg");
            champGrid.getChildren().add(empty);
        } else {
            for (PersonalChampionStats s : visible) {
                champGrid.getChildren().add(buildChampCard(s));
            }
        }
    }

    private List<PersonalChampionStats> sorted(List<PersonalChampionStats> list) {
        Comparator<PersonalChampionStats> cmp = switch (currentSort) {
            case "Winrate (malejąco)"  -> Comparator.comparingDouble(PersonalChampionStats::getWinRate).reversed();
            case "KDA (malejąco)"      -> Comparator.comparingDouble(PersonalChampionStats::getKda).reversed();
            case "Alfabetycznie"       -> Comparator.comparing(PersonalChampionStats::getChampionName);
            default                    -> Comparator.comparingInt(PersonalChampionStats::getGames).reversed();
        };
        return list.stream().sorted(cmp).collect(Collectors.toList());
    }

    private void updateKpi(List<PersonalChampionStats> stats) {
        if (stats == null || stats.isEmpty()) return;

        kpiPlayed.setText(String.valueOf(stats.size()));

        stats.stream().max(Comparator.comparingInt(PersonalChampionStats::getGames))
                .ifPresent(s -> kpiTopChamp.setText(s.getChampionName()));

        stats.stream()
                .filter(s -> !"ALL".equals(s.getLaneFilterRole()))
                .collect(Collectors.groupingBy(PersonalChampionStats::getLaneFilterRole, Collectors.summingInt(PersonalChampionStats::getGames)))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .ifPresent(e -> kpiTopRole.setText(e.getKey()));

        int totalGames = stats.stream().mapToInt(PersonalChampionStats::getGames).sum();
        int totalWins  = stats.stream().mapToInt(PersonalChampionStats::getWins).sum();
        if (totalGames > 0) {
            kpiOverallWr.setText(String.format("%.1f%%", totalWins * 100.0 / totalGames));
        }
    }

    private Node buildChampCard(PersonalChampionStats s) {
        VBox card = new VBox();
        card.getStyleClass().add("champ-card");
        card.setPrefWidth(160);
        card.setMaxWidth(160);

        StackPane portrait = new StackPane();
        portrait.getStyleClass().addAll("champ-portrait", "champ-portrait-bg-" + s.getRoleCssClass());
        portrait.setPrefHeight(140);

        ImageView icon = new ImageView();
        icon.setFitWidth(100);
        icon.setFitHeight(100);
        icon.setPreserveRatio(false);
        icon.setSmooth(true);
        Rectangle clip = new Rectangle(100, 100);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        icon.setClip(clip);
        loadImage(icon, () -> dataDragonService.getChampionImageStream(s.getChampionName()));

        HBox gamesBox = new HBox();
        gamesBox.setAlignment(Pos.TOP_RIGHT);
        StackPane.setAlignment(gamesBox, Pos.TOP_RIGHT);
        gamesBox.setPadding(new Insets(8));
        Label gamesBadge = new Label(s.getGames() + "g");
        gamesBadge.getStyleClass().addAll("role-chip-mini");
        gamesBox.getChildren().add(gamesBadge);

        HBox roleBox = new HBox();
        roleBox.setAlignment(Pos.BOTTOM_LEFT);
        StackPane.setAlignment(roleBox, Pos.BOTTOM_LEFT);
        roleBox.setPadding(new Insets(8));
        Label roleChip = new Label(s.getLaneLabel());
        roleChip.getStyleClass().add("role-chip-mini");
        roleBox.getChildren().add(roleChip);

        portrait.getChildren().addAll(icon, gamesBox, roleBox);

        VBox info = new VBox(4);
        info.getStyleClass().add("champ-info");

        Label nameLabel = new Label(s.getChampionName());
        nameLabel.getStyleClass().add("champ-name");

        HBox wrRow = new HBox(6);
        wrRow.setAlignment(Pos.CENTER_LEFT);
        double wr = s.getWinRate();
        String wrStyle = wr >= 55 ? "text-success" : wr >= 45 ? "text-gold" : "text-danger";
        Label wrLabel = new Label(String.format("%.1f%% WR", wr));
        wrLabel.getStyleClass().addAll(wrStyle, "text-bold", "text-sm");
        Label sep = new Label("·");
        sep.getStyleClass().addAll("text-muted", "text-sm");
        Label kdaLabel = new Label(String.format("%.2f KDA", s.getKda()));
        kdaLabel.getStyleClass().addAll("text-muted", "text-sm");
        wrRow.getChildren().addAll(wrLabel, sep, kdaLabel);

        HBox wlRow = new HBox(4);
        wlRow.setAlignment(Pos.CENTER_LEFT);
        Label wLabel = new Label(s.getWins() + "W");
        wLabel.getStyleClass().addAll("text-success", "text-sm");
        Label dashLabel = new Label("/");
        dashLabel.getStyleClass().addAll("text-muted", "text-sm");
        Label lLabel = new Label(s.getLosses() + "L");
        lLabel.getStyleClass().addAll("text-danger", "text-sm");
        wlRow.getChildren().addAll(wLabel, dashLabel, lLabel);

        info.getChildren().addAll(nameLabel, wrRow, wlRow);
        card.getChildren().addAll(portrait, info);
        return card;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    @FunctionalInterface
    private interface ImageFetcher { InputStream fetch() throws Exception; }

    private void loadImage(ImageView target, ImageFetcher fetcher) {
        double w = target.getFitWidth();
        double h = target.getFitHeight();
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                InputStream is = fetcher.fetch();
                return (w > 0 && h > 0) ? new Image(is, w, h, false, true) : new Image(is);
            }
        };
        task.setOnSucceeded(e -> target.setImage(task.getValue()));
        daemon(task);
    }

    private void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
