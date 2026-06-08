package com.yuhyfe.loldraftanalyzer.controller;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.AppSettings;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.ranked.LeagueEntry;
import com.yuhyfe.loldraftanalyzer.model.ranked.RankedEntry;
import com.yuhyfe.loldraftanalyzer.model.ranked.RankedStats;
import com.yuhyfe.loldraftanalyzer.service.DataDragonService;
import com.yuhyfe.loldraftanalyzer.service.RiotApiClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class RankingsController {

    @FXML private Button btnSolo;
    @FXML private Button btnFlex;
    @FXML private ComboBox<String> tierCombo;
    @FXML private VBox contentArea;
    @FXML private Label statusLabel;

    private static final Gson          GSON        = new Gson();
    private static final HttpClient    HTTP        = HttpClient.newHttpClient();
    private final        RiotApiClient riotClient  = new RiotApiClient();
    private final        DataDragonService ddragon = new DataDragonService();

    private String currentQueue = "RANKED_SOLO_5x5";
    private String currentTier  = "Challenger";

    @FXML
    public void initialize() {
        tierCombo.setValue("Challenger");
        tierCombo.setOnAction(e -> { currentTier = tierCombo.getValue(); loadData(); });
        loadData();
    }

    @FXML private void handleRefresh() { loadData(); }
    @FXML private void handleSolo() { setQueue("RANKED_SOLO_5x5", btnSolo); }
    @FXML private void handleFlex() { setQueue("RANKED_FLEX_SR",  btnFlex); }

    private void setQueue(String queue, Button active) {
        currentQueue = queue;
        btnSolo.getStyleClass().remove("active");
        btnFlex.getStyleClass().remove("active");
        active.getStyleClass().add("active");
        loadData();
    }

    private void loadData() {
        setStatus("Ładowanie…");
        contentArea.getChildren().clear();
        Label loading = new Label("Ładowanie…");
        loading.getStyleClass().addAll("text-muted", "text-sm");
        contentArea.getChildren().add(loading);

        if (AppSettings.get().hasApiKey()) {
            loadLeaderboard();
        } else {
            loadUserStats();
        }
    }

    private void loadLeaderboard() {
        String tier = apiTierKey(currentTier);
        Task<List<LeagueEntry>> task = new Task<>() {
            @Override
            protected List<LeagueEntry> call() throws Exception {
                setStatus("Pobieranie listy " + currentTier + "…");
                List<LeagueEntry> entries = riotClient.getLeagueTopEntries(currentQueue, tier);
                setStatus("Rozwiązywanie nazw graczy (top 10)…");
                riotClient.resolveAccountNames(entries, 10);
                return entries;
            }
        };
        task.setOnSucceeded(e -> {
            List<LeagueEntry> entries = task.getValue();
            Platform.runLater(() -> renderLeaderboard(entries));
            setStatus("Załadowano " + entries.size() + " graczy  ·  " + currentTier
                    + "  ·  " + queueLabel(currentQueue));
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "nieznany błąd";
            setStatus("Błąd: " + msg);
            Platform.runLater(() -> {
                contentArea.getChildren().setAll(errorLabel(msg));
            });
        });
        daemon(task);
    }

    private void renderLeaderboard(List<LeagueEntry> entries) {
        contentArea.getChildren().clear();
        if (entries.isEmpty()) {
            contentArea.getChildren().add(errorLabel("Brak danych dla " + currentTier));
            return;
        }

        if (entries.size() >= 3) {
            contentArea.getChildren().add(buildPodium(entries));
        }

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label cardTitle = new Label("Pełny ranking");
        cardTitle.getStyleClass().add("section-title");
        HBox.setHgrow(cardTitle, Priority.ALWAYS);
        Label countLabel = new Label("Łącznie: " + entries.size() + " graczy");
        countLabel.getStyleClass().addAll("text-muted", "text-sm");
        cardHeader.getChildren().addAll(cardTitle, countLabel);

        HBox colHeaders = new HBox(20);
        colHeaders.setAlignment(Pos.CENTER_LEFT);
        colHeaders.setPadding(new Insets(0, 16, 0, 16));
        colHeaders.getChildren().addAll(
            colHeader("#",         48),
            colHeader("GRACZ",     -1),
            colHeader("TIER / LP", 180),
            colHeader("WIN RATE",  110),
            colHeader("GRY",       80)
        );

        VBox rowsBox = new VBox(8);
        for (int i = 0; i < entries.size(); i++) {
            rowsBox.getChildren().add(buildRankRow(i + 1, entries.get(i)));
        }

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        card.getChildren().addAll(cardHeader, colHeaders, scroll);
        contentArea.getChildren().add(card);
    }

    private Node buildPodium(List<LeagueEntry> entries) {
        HBox podium = new HBox(24);
        podium.setAlignment(Pos.CENTER);

        podium.getChildren().addAll(
            buildPodiumCard(2, entries.get(1), "podium-card podium-2", "rank-position rank-position-silver"),
            buildPodiumCard(1, entries.get(0), "podium-card podium-1", "rank-position rank-position-gold"),
            buildPodiumCard(3, entries.get(2), "podium-card podium-3", "rank-position rank-position-bronze")
        );
        return podium;
    }

    private Node buildPodiumCard(int rank, LeagueEntry e,
                                  String cardStyles, String rankStyles) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().setAll(cardStyles.split(" "));
        HBox.setHgrow(card, Priority.ALWAYS);

        Label rankLabel = new Label(String.valueOf(rank));
        for (String s : rankStyles.split(" ")) rankLabel.getStyleClass().add(s);
        int sz = rank == 1 ? 68 : 60;
        rankLabel.setStyle("-fx-font-size: " + (rank == 1 ? "28" : "24") + "px;"
                + "-fx-min-width: " + sz + "; -fx-min-height: " + sz + ";"
                + "-fx-pref-width: " + sz + "; -fx-pref-height: " + sz + ";");

        int avatarSz = rank == 1 ? 110 : 90;
        String borderColor = switch (rank) {
            case 1 -> "#FFD700";
            case 2 -> "#C0C0C0";
            default -> "#CD7F32";
        };
        javafx.scene.layout.StackPane avatarPane = new javafx.scene.layout.StackPane();
        avatarPane.setPrefSize(avatarSz, avatarSz);
        avatarPane.setMaxSize(avatarSz, avatarSz);

        Region avatarBg = new Region();
        avatarBg.setStyle("-fx-background-color: #14253F; -fx-background-radius: 999;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 3; -fx-border-radius: 999;");
        avatarBg.setPrefSize(avatarSz, avatarSz);

        ImageView avatarImg = new ImageView();
        avatarImg.setFitWidth(avatarSz - 6);
        avatarImg.setFitHeight(avatarSz - 6);
        avatarImg.setPreserveRatio(false);
        Circle avatarClip = new Circle((avatarSz - 6) / 2.0);
        avatarClip.setCenterX((avatarSz - 6) / 2.0);
        avatarClip.setCenterY((avatarSz - 6) / 2.0);
        avatarImg.setClip(avatarClip);
        if (e.getProfileIconId() > 0) {
            loadProfileIcon(avatarImg, e.getProfileIconId(), avatarSz - 6);
        }
        avatarPane.getChildren().addAll(avatarBg, avatarImg);

        Label nameLabel = new Label(e.displayName());
        nameLabel.getStyleClass().add("summoner-name");
        nameLabel.setStyle("-fx-font-size: " + (rank == 1 ? "26" : "22") + "px;");

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER);
        Label tierChip = new Label(e.getTier().substring(0, 1).toUpperCase() + e.getTier().substring(1).toLowerCase());
        tierChip.getStyleClass().add("chip-gold");
        Label lpChip = new Label(e.getLeaguePoints() + " LP");
        lpChip.getStyleClass().add("chip");
        chips.getChildren().addAll(tierChip, lpChip);

        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);
        String wrStyle = e.winRatePct() >= 55 ? "text-success" : e.winRatePct() >= 50 ? "text-gold" : "text-danger";
        stats.getChildren().addAll(
            statBox(String.format("%.1f%%", e.winRatePct()), "WIN RATE", wrStyle),
            statBox(String.valueOf(e.totalGames()), "GRY", "text-bold")
        );

        card.getChildren().addAll(rankLabel, avatarPane, nameLabel, chips, stats);
        return card;
    }

    private Node statBox(String value, String label, String valueStyle) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label val = new Label(value);
        val.getStyleClass().addAll(valueStyle, "text-bold", "text-lg");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("kpi-label");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private Node buildRankRow(int rank, LeagueEntry e) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("rank-row");

        Label rankLbl = new Label(String.valueOf(rank));
        rankLbl.getStyleClass().add("rank-position");
        rankLbl.setPrefWidth(48);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        iconPane.setPrefSize(40, 40);
        iconPane.setMaxSize(40, 40);
        Region iconBg = new Region();
        iconBg.setStyle("-fx-background-color: #14253F; -fx-background-radius: 999;");
        iconBg.setPrefSize(40, 40);
        ImageView iconImg = new ImageView();
        iconImg.setFitWidth(40);
        iconImg.setFitHeight(40);
        Circle iconClip = new Circle(20, 20, 20);
        iconImg.setClip(iconClip);
        if (e.getProfileIconId() > 0) loadProfileIcon(iconImg, e.getProfileIconId(), 40);
        iconPane.getChildren().addAll(iconBg, iconImg);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name = new Label(e.displayName());
        name.getStyleClass().addAll("text-bold", "text-gold");
        Label sub  = new Label(capitalize(e.getTier()));
        sub.getStyleClass().addAll("text-muted", "text-sm");
        nameBox.getChildren().addAll(name, sub);

        VBox tierBox = new VBox(2);
        tierBox.setPrefWidth(180);
        Label tierLbl = new Label(capitalize(e.getTier()));
        tierLbl.getStyleClass().addAll("text-bold", "text-gold");
        Label lpLbl   = new Label(e.getLeaguePoints() + " LP");
        lpLbl.getStyleClass().addAll("text-muted", "text-sm");
        tierBox.getChildren().addAll(tierLbl, lpLbl);

        String wrStyle = e.winRatePct() >= 55 ? "text-success" : e.winRatePct() >= 50 ? "text-gold" : "text-danger";
        Label wrLbl = new Label(String.format("%.1f%%", e.winRatePct()));
        wrLbl.getStyleClass().addAll(wrStyle, "text-bold");
        wrLbl.setPrefWidth(110);

        Label gamesLbl = new Label(String.valueOf(e.totalGames()));
        gamesLbl.getStyleClass().add("text-bold");
        gamesLbl.setPrefWidth(80);

        row.getChildren().addAll(rankLbl, iconPane, nameBox, tierBox, wrLbl, gamesLbl);
        return row;
    }

    private void loadUserStats() {
        Task<RankedStats> task = new Task<>() {
            @Override
            protected RankedStats call() throws Exception {
                String json = LcuConnector.getInstance().get(
                        "/lol-ranked/v1/current-ranked-stats");
                return GSON.fromJson(json, RankedStats.class);
            }
        };
        task.setOnSucceeded(e -> {
            RankedStats stats = task.getValue();
            Platform.runLater(() -> renderUserStats(stats));
            setStatus("Dane z LCU  ·  Dodaj klucz Riot API w Ustawieniach, aby zobaczyć ranking serwera");
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "nieznany błąd";
            setStatus("Błąd: " + msg);
            Platform.runLater(() -> contentArea.getChildren().setAll(errorLabel(msg)));
        });
        daemon(task);
    }

    private void renderUserStats(RankedStats stats) {
        contentArea.getChildren().clear();

        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: rgba(200,170,110,0.08); -fx-padding: 12 16 12 16;"
                + "-fx-background-radius: 8; -fx-border-radius: 8;"
                + "-fx-border-color: rgba(200,170,110,0.25); -fx-border-width: 1;");
        Label bannerLbl = new Label("Brak klucza Riot API — poniżej Twoje własne statystyki rankingowe. "
                + "Dodaj klucz w Ustawieniach (⚙), aby zobaczyć leaderboard serwera.");
        bannerLbl.getStyleClass().addAll("text-muted", "text-sm");
        bannerLbl.setWrapText(true);
        HBox.setHgrow(bannerLbl, Priority.ALWAYS);
        banner.getChildren().add(bannerLbl);
        contentArea.getChildren().add(banner);

        HBox cardsRow = new HBox(24);
        RankedEntry solo = stats != null ? stats.getSolo() : new RankedEntry();
        RankedEntry flex = stats != null ? stats.getFlex() : new RankedEntry();
        cardsRow.getChildren().addAll(
            buildUserRankedCard("Solo / Duo", solo),
            buildUserRankedCard("Flex 5v5",   flex)
        );
        contentArea.getChildren().add(cardsRow);
    }

    private Node buildUserRankedCard(String queueLabel, RankedEntry entry) {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setAlignment(Pos.TOP_CENTER);

        Label title = new Label(queueLabel);
        title.getStyleClass().add("section-title");

        if (entry.isUnranked()) {
            Label unranked = new Label("Nieukwalifikowany");
            unranked.getStyleClass().addAll("text-muted", "kpi-value");
            card.getChildren().addAll(title, unranked);
            return card;
        }

        String tierName = capitalize(entry.getTier())
                + (entry.getDivision() != null && !entry.getDivision().isEmpty()
                   ? " " + entry.getDivision() : "");

        Label tierLabel = new Label(tierName);
        tierLabel.getStyleClass().addAll("kpi-value", "kpi-value-success");

        Label lpLabel = new Label(entry.getLeaguePoints() + " LP");
        lpLabel.getStyleClass().addAll("text-bold", "text-lg", "text-gold");

        HBox stats = new HBox(32);
        stats.setAlignment(Pos.CENTER);
        double wr = entry.winRatePct();
        String wrStyle = wr >= 55 ? "kpi-value-success" : wr >= 50 ? "kpi-value-warn" : "kpi-value-danger";
        stats.getChildren().addAll(
            kpiBox(entry.getWins() + "W / " + entry.getLosses() + "L", "BILANS"),
            kpiBox(entry.winRatePct() + "%", "WIN RATE", wrStyle),
            kpiBox(String.valueOf(entry.totalGames()), "GRY")
        );

        card.getChildren().addAll(title, tierLabel, lpLabel, stats);
        return card;
    }

    private Node kpiBox(String value, String label) { return kpiBox(value, label, ""); }

    private Node kpiBox(String value, String label, String valueStyle) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        Label val = new Label(value);
        val.getStyleClass().add("kpi-value");
        if (!valueStyle.isEmpty()) val.getStyleClass().add(valueStyle);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("kpi-label");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private Label colHeader(String text, double width) {
        Label l = new Label(text);
        l.getStyleClass().add("kpi-label");
        if (width < 0) {
            HBox.setHgrow(l, Priority.ALWAYS);
            l.setMaxWidth(Double.MAX_VALUE);
        } else {
            l.setPrefWidth(width);
        }
        return l;
    }

    private Label errorLabel(String msg) {
        Label l = new Label("Błąd: " + msg);
        l.getStyleClass().addAll("text-danger", "text-sm");
        l.setWrapText(true);
        return l;
    }

    private String apiTierKey(String display) {
        return switch (display) {
            case "Grandmaster" -> "GRANDMASTER";
            case "Master"      -> "MASTER";
            default            -> "CHALLENGER";
        };
    }

    private String queueLabel(String queue) {
        return "RANKED_FLEX_SR".equals(queue) ? "Flex 5v5" : "Solo/Duo";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void loadProfileIcon(ImageView target, int iconId, double size) {
        Task<Image> t = new Task<>() {
            @Override
            protected Image call() throws Exception {
                String version = ddragon.getLatestVersion();
                String url = "https://ddragon.leagueoflegends.com/cdn/"
                        + version + "/img/profileicon/" + iconId + ".png";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url)).GET().build();
                HttpResponse<InputStream> resp = HTTP.send(req,
                        HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) return null;
                return new Image(resp.body(), size, size, false, true);
            }
        };
        t.setOnSucceeded(e -> { if (t.getValue() != null) target.setImage(t.getValue()); });
        daemon(t);
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (statusLabel != null) statusLabel.setText(msg); });
    }

    private void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
