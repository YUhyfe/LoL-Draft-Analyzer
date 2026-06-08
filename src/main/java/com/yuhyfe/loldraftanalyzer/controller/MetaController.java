package com.yuhyfe.loldraftanalyzer.controller;

import com.yuhyfe.loldraftanalyzer.model.meta.ChampFrequency;
import com.yuhyfe.loldraftanalyzer.model.meta.MetaAggregation;
import com.yuhyfe.loldraftanalyzer.service.DataDragonService;
import com.yuhyfe.loldraftanalyzer.service.MetaService;
import com.yuhyfe.loldraftanalyzer.service.SummonerService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

public class MetaController {

    private static final Logger LOG = AppLogger.get(MetaController.class);

    @FXML private Label kpiGames;
    @FXML private Label kpiPatch;
    @FXML private Label kpiTopPick;
    @FXML private Label kpiTopBan;

    @FXML private Label cacheInfoLabel;
    @FXML private Label dataSourceLabel;

    @FXML private VBox pickList;
    @FXML private VBox banList;
    @FXML private VBox wrList;

    @FXML private Label statusLabel;

    private final DataDragonService dataDragonService = new DataDragonService();
    private final SummonerService   summonerService   = new SummonerService();
    private final MetaService       metaService       = new MetaService(dataDragonService);

    private Task<?> currentTask;

    @FXML
    public void initialize() {
        loadMeta();
    }

    @FXML
    private void handleRefresh() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
        loadMeta();
    }

    private void loadMeta() {
        setStatus("Inicjalizacja…");
        setLoadingState();

        Task<MetaAggregation> task = new Task<>() {
            @Override
            protected MetaAggregation call() throws Exception {
                String puuid = summonerService.getCurrentSummoner().getPuuid();
                return metaService.aggregate(puuid,
                        msg -> Platform.runLater(() -> setStatus(msg)));
            }
        };
        currentTask = task;

        task.setOnSucceeded(e -> {
            MetaAggregation meta = task.getValue();
            populateKpi(meta);
            populateCacheInfo(meta);
            populateList(pickList, meta.getMostPicked(), ListType.PICK);
            populateList(banList,  meta.getMostBanned(),  ListType.BAN);
            populateList(wrList,   meta.getBestWinRate(), ListType.WR);
            String src = meta.isFromCache() ? "Riot API" : "LCU";
            setStatus("Analiza " + meta.getTotalGamesAnalyzed()
                    + " meczow  |  Patch " + meta.getPatchVersion()
                    + "  |  Zrodlo: " + src);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "nieznany blad";
            setStatus("Blad: " + msg);
            LOG.warning("loadMeta error: " + (ex != null ? ex.getMessage() : "unknown"));

            Label errLabel = new Label("Blad ladowania: " + msg);
            errLabel.getStyleClass().addAll("text-danger", "text-sm");
            errLabel.setWrapText(true);
            pickList.getChildren().setAll(errLabel);
            banList.getChildren().clear();
            wrList.getChildren().clear();

            if (cacheInfoLabel != null)
                cacheInfoLabel.setText("Sprawdz klucz API i region w Ustawieniach (przycisk zybki w gornym pasku)");
            if (dataSourceLabel != null)
                dataSourceLabel.setText("");
        });

        daemon(task);
    }

    private enum ListType { PICK, BAN, WR }

    private void populateKpi(MetaAggregation meta) {
        kpiGames.setText(meta.getTotalGamesAnalyzed() == 0 ? "—"
                : String.valueOf(meta.getTotalGamesAnalyzed()));
        kpiPatch.setText(meta.getPatchVersion().isBlank() ? "—" : meta.getPatchVersion());

        kpiTopPick.setText(meta.getMostPicked().isEmpty() ? "—"
                : meta.getMostPicked().get(0).getChampionName());
        kpiTopBan.setText(meta.getMostBanned().isEmpty() ? "—"
                : meta.getMostBanned().get(0).getChampionName());
    }

    private void populateCacheInfo(MetaAggregation meta) {
        if (cacheInfoLabel == null || dataSourceLabel == null) return;

        if (meta.isFromCache()) {
            String lastUpd = meta.getLastUpdated().isBlank() ? "" : " · " + meta.getLastUpdated();
            cacheInfoLabel.setText("Cache: " + meta.getCachedGames() + " mecze" + lastUpd);
            dataSourceLabel.setText("Zrodlo: Riot API");
            dataSourceLabel.getStyleClass().removeAll("text-muted", "text-danger");
            dataSourceLabel.getStyleClass().add("text-success");
        } else if (meta.getTotalGamesAnalyzed() > 0) {
            cacheInfoLabel.setText("Dane z LCU (~" + meta.getTotalGamesAnalyzed() + " mecze, tylko Twoje gry)");
            dataSourceLabel.setText("Brak klucza Riot API — ustaw w ustawieniach dla pelnej statystyki");
            dataSourceLabel.getStyleClass().removeAll("text-success", "text-danger");
            dataSourceLabel.getStyleClass().addAll("text-muted");
        } else {
            cacheInfoLabel.setText("Brak danych");
            dataSourceLabel.setText(meta.getLastUpdated().isBlank() ? ""
                    : meta.getLastUpdated());
            dataSourceLabel.getStyleClass().removeAll("text-success");
            dataSourceLabel.getStyleClass().add("text-muted");
        }
    }

    private void populateList(VBox container, List<ChampFrequency> list, ListType type) {
        container.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Brak wystarczajacych danych");
            empty.getStyleClass().addAll("text-muted", "text-sm");
            container.getChildren().add(empty);
            return;
        }
        double maxVal = list.get(0).getPct();
        int rank = 1;
        for (ChampFrequency cf : list) {
            container.getChildren().add(buildRow(rank++, cf, maxVal, type));
        }
    }

    private Node buildRow(int rank, ChampFrequency cf, double maxPct, ListType type) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label rankLabel = new Label(rank + ".");
        rankLabel.getStyleClass().addAll("text-muted", "text-sm");
        rankLabel.setMinWidth(28);
        rankLabel.setPrefWidth(28);

        ImageView icon = new ImageView();
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        icon.setPreserveRatio(false);
        icon.setSmooth(true);
        Rectangle clip = new Rectangle(32, 32);
        clip.setArcWidth(7);
        clip.setArcHeight(7);
        icon.setClip(clip);
        loadImage(icon, () -> dataDragonService.getChampionImageStream(cf.getChampionName()));

        VBox nameBar = new VBox(4);
        HBox.setHgrow(nameBar, javafx.scene.layout.Priority.ALWAYS);

        Label name = new Label(cf.getChampionName());
        name.getStyleClass().add("text-bold");

        StackPane barContainer = new StackPane();
        barContainer.setPrefHeight(6);
        barContainer.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.Region track = new javafx.scene.layout.Region();
        track.getStyleClass().add("bar-track");
        track.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
        String fillStyle = switch (type) {
            case PICK -> "bar-fill-info";
            case BAN  -> "bar-fill-danger";
            case WR   -> "bar-fill-success";
        };
        fill.getStyleClass().add(fillStyle);
        double barWidth = maxPct > 0 ? (cf.getPct() / maxPct) * 280 : 0;
        fill.setPrefWidth(barWidth);
        fill.setMaxWidth(barWidth);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        barContainer.getChildren().addAll(track, fill);
        nameBar.getChildren().addAll(name, barContainer);

        String valueText = switch (type) {
            case PICK, BAN -> String.format("%.1f%%", cf.getPct());
            case WR        -> String.format("%.0f%%", cf.getPct());
        };
        String valueStyle = switch (type) {
            case PICK -> "text-gold";
            case BAN  -> "text-danger";
            case WR   -> cf.getPct() >= 55 ? "text-success"
                       : cf.getPct() >= 45 ? "text-gold"
                       : "text-danger";
        };
        Label pctLabel = new Label(valueText);
        pctLabel.getStyleClass().addAll(valueStyle, "text-bold");
        pctLabel.setMinWidth(56);
        pctLabel.setPrefWidth(56);
        pctLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(rankLabel, icon, nameBar, pctLabel);
        return row;
    }

    private void setLoadingState() {
        for (VBox list : new VBox[]{pickList, banList, wrList}) {
            list.getChildren().clear();
            Label lbl = new Label("Ladowanie…");
            lbl.getStyleClass().addAll("text-muted", "text-sm");
            list.getChildren().add(lbl);
        }
        if (kpiGames != null)  kpiGames.setText("—");
        if (kpiPatch != null)  kpiPatch.setText("—");
        if (kpiTopPick != null) kpiTopPick.setText("—");
        if (kpiTopBan != null)  kpiTopBan.setText("—");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    @FunctionalInterface
    private interface ImageFetcher { InputStream fetch() throws Exception; }

    private void loadImage(ImageView target, ImageFetcher fetcher) {
        double w = target.getFitWidth();
        double h = target.getFitHeight();
        Task<Image> t = new Task<>() {
            @Override protected Image call() throws Exception {
                InputStream is = fetcher.fetch();
                return (w > 0 && h > 0) ? new Image(is, w, h, false, true) : new Image(is);
            }
        };
        t.setOnSucceeded(e -> target.setImage(t.getValue()));
        daemon(t);
    }

    private void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
