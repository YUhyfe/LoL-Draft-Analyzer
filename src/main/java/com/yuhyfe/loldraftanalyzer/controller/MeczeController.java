package com.yuhyfe.loldraftanalyzer.controller;

import com.yuhyfe.loldraftanalyzer.model.match.MatchSummary;
import com.yuhyfe.loldraftanalyzer.model.summoner.Summoner;
import com.yuhyfe.loldraftanalyzer.service.DataDragonService;
import com.yuhyfe.loldraftanalyzer.service.MatchHistoryService;
import com.yuhyfe.loldraftanalyzer.service.SummonerService;
import com.yuhyfe.loldraftanalyzer.util.MatchUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class MeczeController {

    @FXML private Button btnAll;
    @FXML private Button btnSolo;
    @FXML private Button btnFlex;
    @FXML private Button btnNormal;
    @FXML private Button btnAram;
    @FXML private ComboBox<String> resultCombo;
    @FXML private Label kpiRecord;
    @FXML private Label kpiWr;
    @FXML private Label kpiKda;
    @FXML private Label kpiAvgTime;
    @FXML private VBox matchListContainer;
    @FXML private Label statusLabel;

    private static final int FETCH_COUNT = 100;

    private final DataDragonService  dataDragonService  = new DataDragonService();
    private final SummonerService    summonerService    = new SummonerService();
    private final MatchHistoryService matchHistoryService = new MatchHistoryService(dataDragonService);

    private List<MatchSummary> allMatches;

    private int    filterQueueId = -1;
    private String filterResult  = "Wszystkie";

    @FXML
    public void initialize() {
        resultCombo.setValue("Wszystkie");
        resultCombo.setOnAction(e -> { filterResult = resultCombo.getValue(); applyFiltersAndRender(); });
        loadMatches();
    }

    @FXML private void handleRefresh() { loadMatches(); }
    @FXML private void handleAll()    { setQueueFilter(-1,  btnAll); }
    @FXML private void handleSolo()   { setQueueFilter(420, btnSolo); }
    @FXML private void handleFlex()   { setQueueFilter(440, btnFlex); }
    @FXML private void handleNormal() { setQueueFilter(400, btnNormal); }
    @FXML private void handleAram()   { setQueueFilter(450, btnAram); }

    private void setQueueFilter(int queueId, Button active) {
        filterQueueId = queueId;
        for (Button b : new Button[]{btnAll, btnSolo, btnFlex, btnNormal, btnAram})
            b.getStyleClass().remove("active");
        active.getStyleClass().add("active");
        applyFiltersAndRender();
    }

    private void loadMatches() {
        setStatus("Ładowanie historii meczów…");
        matchListContainer.getChildren().clear();
        Label loading = new Label("Ładowanie…");
        loading.getStyleClass().addAll("text-muted", "text-sm");
        matchListContainer.getChildren().add(loading);

        Task<List<MatchSummary>> task = new Task<>() {
            @Override
            protected List<MatchSummary> call() throws Exception {
                Summoner me = summonerService.getCurrentSummoner();
                return matchHistoryService.getRecentMatches(me.getPuuid(), me.getGameName(), FETCH_COUNT);
            }
        };
        task.setOnSucceeded(e -> {
            allMatches = task.getValue();
            applyFiltersAndRender();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "nieznany błąd";
            setStatus("Błąd: " + msg);
            Platform.runLater(() -> matchListContainer.getChildren().setAll(errorLabel(msg)));
        });
        daemon(task);
    }

    private void applyFiltersAndRender() {
        if (allMatches == null) return;

        List<MatchSummary> filtered = allMatches.stream()
            .filter(m -> filterQueueId == -1 || m.getQueueId() == filterQueueId)
            .filter(m -> switch (filterResult) {
                case "Tylko wygrane"   -> m.isWin();
                case "Tylko przegrane" -> !m.isWin();
                default -> true;
            })
            .collect(Collectors.toList());

        updateKpis(filtered);
        renderList(filtered);

        String qLabel = filterQueueId == -1 ? "wszystkie kolejki" : MatchUtils.queueName(filterQueueId);
        setStatus(allMatches.size() + " meczów w historii  ·  " + filtered.size() + " po filtrowaniu  ·  " + qLabel);
    }

    private void updateKpis(List<MatchSummary> matches) {
        int wins = 0, losses = 0;
        long totalK = 0, totalD = 0, totalA = 0, totalDur = 0;

        for (MatchSummary m : matches) {
            if (m.isWin()) wins++; else losses++;
            totalK   += m.getKills();
            totalD   += m.getDeaths();
            totalA   += m.getAssists();
            totalDur += m.getDurationSeconds();
        }

        int total = wins + losses;
        double wrNum = total > 0 ? wins * 100.0 / total : 0;
        String record  = wins + "W / " + losses + "L";
        String wr      = total > 0 ? String.format("%.1f%%", wrNum) : "—";
        String kda     = total == 0 ? "—"
                       : totalD == 0 ? "Perfect"
                       : String.format("%.2f", (totalK + totalA) / (double) totalD);
        String avgTime = total > 0 ? MatchUtils.formatDuration(totalDur / total) : "—";

        String wrClass = wrNum >= 55 ? "kpi-value-success"
                       : wrNum >= 45 ? "kpi-value-warn"
                       : (total > 0  ? "kpi-value-danger" : "");

        Platform.runLater(() -> {
            kpiRecord.setText(record);
            kpiRecord.getStyleClass().removeAll("kpi-value-success", "kpi-value-danger");
            if (total > 0)
                kpiRecord.getStyleClass().add(wrNum >= 50 ? "kpi-value-success" : "kpi-value-danger");

            kpiWr.setText(wr);
            kpiWr.getStyleClass().removeAll("kpi-value-success", "kpi-value-danger", "kpi-value-warn");
            if (!wrClass.isEmpty()) kpiWr.getStyleClass().add(wrClass);

            kpiKda.setText(kda);
            kpiAvgTime.setText(avgTime);
        });
    }

    private void renderList(List<MatchSummary> matches) {
        Platform.runLater(() -> {
            matchListContainer.getChildren().clear();
            if (matches.isEmpty()) {
                Label empty = new Label("Brak meczów spełniających kryteria filtrowania");
                empty.getStyleClass().addAll("text-muted", "text-sm");
                matchListContainer.getChildren().add(empty);
                return;
            }
            for (MatchSummary m : matches)
                matchListContainer.getChildren().add(buildRow(m));
        });
    }

    private Node buildRow(MatchSummary m) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().setAll("match-detail-row", m.isWin() ? "match-detail-row-win" : "match-detail-row-loss");

        VBox resultBox = new VBox(6);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPrefWidth(70);
        Label resultChip = new Label(m.isWin() ? "WIN" : "LOSS");
        resultChip.getStyleClass().add(m.isWin() ? "win-chip" : "loss-chip");
        Label queueChip = new Label(m.getQueueName());
        queueChip.getStyleClass().add("queue-chip");
        resultBox.getChildren().addAll(resultChip, queueChip);

        ImageView icon = new ImageView();
        icon.setFitWidth(64);
        icon.setFitHeight(64);
        icon.setPreserveRatio(false);
        icon.setSmooth(true);
        Rectangle clip = new Rectangle(64, 64);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        icon.setClip(clip);
        if (m.getChampionName() != null && !m.getChampionName().isBlank()) {
            loadImage(icon, () -> dataDragonService.getChampionImageStream(m.getChampionName()));
        }

        VBox nameBox = new VBox(4);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.setPrefWidth(180);
        Label champLabel = new Label(m.getChampionName() != null ? m.getChampionName() : "Nieznany");
        champLabel.getStyleClass().addAll("text-bold", "text-gold", "text-lg");
        String laneStr = m.getLaneDisplay().equals("—") ? "" : m.getLaneDisplay();
        Label subLabel = new Label((laneStr.isEmpty() ? "" : laneStr + "  ·  ") + m.getFormattedTimeAgo());
        subLabel.getStyleClass().addAll("text-muted", "text-sm");
        Label mapLabel = new Label(m.getQueueId() == 450 ? "Howling Abyss" : "Summoner's Rift");
        mapLabel.getStyleClass().addAll("text-muted", "text-sm");
        nameBox.getChildren().addAll(champLabel, subLabel, mapLabel);

        VBox kdaBox = new VBox(4);
        kdaBox.setAlignment(Pos.CENTER);
        kdaBox.setPrefWidth(130);
        Label raw = new Label(m.getKdaString());
        raw.getStyleClass().addAll("text-bold", "text-lg");
        double kdaVal = m.getDeaths() == 0
                ? (m.getKills() + m.getAssists())
                : (m.getKills() + m.getAssists()) / (double) m.getDeaths();
        String kdaStr   = m.getDeaths() == 0 ? "KDA Perfect" : String.format("KDA %.2f", kdaVal);
        String kdaStyle = kdaVal >= 3 ? "text-success" : kdaVal >= 2 ? "text-gold" : "text-danger";
        Label kdaLbl = new Label(kdaStr);
        kdaLbl.getStyleClass().addAll(kdaStyle, "text-sm", "text-bold");
        kdaBox.getChildren().addAll(raw, kdaLbl);

        VBox timeBox = new VBox(4);
        timeBox.setAlignment(Pos.CENTER_RIGHT);
        timeBox.setPrefWidth(70);
        Label durLabel = new Label(m.getFormattedDuration());
        durLabel.getStyleClass().add("text-muted");
        timeBox.getChildren().add(durLabel);

        row.getChildren().addAll(resultBox, icon, nameBox, kdaBox, timeBox);
        return row;
    }

    private Label errorLabel(String msg) {
        Label l = new Label("Błąd ładowania: " + msg);
        l.getStyleClass().addAll("text-danger", "text-sm");
        l.setWrapText(true);
        return l;
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (statusLabel != null) statusLabel.setText(msg); });
    }

    @FunctionalInterface
    private interface ImageFetcher { InputStream fetch() throws Exception; }

    private void loadImage(ImageView target, ImageFetcher fetcher) {
        double w = target.getFitWidth();
        double h = target.getFitHeight();
        Task<Image> t = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return new Image(fetcher.fetch(), w, h, false, true);
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
