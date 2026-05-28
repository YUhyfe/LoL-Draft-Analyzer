package com.yuhyfe.loldraftanalyzer;

import com.yuhyfe.loldraftanalyzer.model.summoner.*;
import com.yuhyfe.loldraftanalyzer.model.ranked.*;
import com.yuhyfe.loldraftanalyzer.model.match.*;
import com.yuhyfe.loldraftanalyzer.service.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ProfileController {

    // Hero card
    @FXML private Label summonerName;
    @FXML private Label summonerTag;
    @FXML private Label levelChip;
    @FXML private Label rankChip;
    @FXML private Label mainRoleChip;
    @FXML private Label platformChip;
    @FXML private Label lastUpdateLabel;
    @FXML private ImageView avatarImage;

    // Rank cards
    @FXML private Label soloTier;
    @FXML private Label soloLp;
    @FXML private Label soloStats;
    @FXML private ImageView soloEmblem;
    @FXML private Label flexTier;
    @FXML private Label flexLp;
    @FXML private Label flexStats;
    @FXML private ImageView flexEmblem;

    // KPI row
    @FXML private Label kpiWinRate;
    @FXML private Label kpiKda;
    @FXML private Label kpiGames;
    @FXML private Label kpiRole;

    // Match list
    @FXML private VBox matchListContainer;

    // Top champions
    @FXML private VBox topChampionsContainer;

    // Status bar
    @FXML private Label statusLabel;

    private final DataDragonService    dataDragonService    = new DataDragonService();
    private final SummonerService      summonerService      = new SummonerService();
    private final RankedService        rankedService        = new RankedService();
    private final MatchHistoryService  matchHistoryService  = new MatchHistoryService(dataDragonService);

    @FXML
    public void initialize() {
        avatarImage.setClip(new Circle(58, 58, 58));
        loadAll();
    }

    @FXML
    private void handleRefresh() {
        lastUpdateLabel.setText("odświeżanie...");
        loadAll();
    }

    private void loadAll() {
        loadSummoner();
        loadRanked();
    }

    // ------------------------------------------------------------------ summoner

    private void loadSummoner() {
        statusLabel.setText("v0.1.0 · LCU: łączenie...");

        Task<Summoner> task = new Task<>() {
            @Override
            protected Summoner call() throws Exception {
                return summonerService.getCurrentSummoner();
            }
        };

        task.setOnSucceeded(e -> {
            Summoner s = task.getValue();
            summonerName.setText(s.getGameName());
            summonerTag.setText("#" + s.getTagLine());
            levelChip.setText("LVL " + s.getSummonerLevel());
            if (s.getPlatformId() != null && !s.getPlatformId().isBlank()) {
                platformChip.setText(s.getPlatformId().toUpperCase());
            }
            statusLabel.setText("v0.1.0 · LCU: połączono");
            updateLastUpdateLabel();
            loadImage(avatarImage, () -> dataDragonService.getProfileIconStream(s.getProfileIconId()));
            loadMatches(s.getPuuid());
        });

        task.setOnFailed(e -> {
            summonerName.setText("—");
            summonerTag.setText("");
            levelChip.setText("LVL —");
            statusLabel.setText("v0.1.0 · LCU: brak połączenia");
        });

        daemon(task);
    }

    // ------------------------------------------------------------------ ranked

    private void loadRanked() {
        Task<RankedStats> task = new Task<>() {
            @Override
            protected RankedStats call() throws Exception {
                return rankedService.getCurrentRankedStats();
            }
        };

        task.setOnSucceeded(e -> {
            RankedStats stats = task.getValue();
            applyRankCard(stats.getSolo(), soloTier, soloLp, soloStats, soloEmblem);
            applyRankCard(stats.getFlex(), flexTier, flexLp, flexStats, flexEmblem);
            rankChip.setText(formatTier(stats.getSolo()));
            applyRankedKpi(stats.getSolo());
        });

        task.setOnFailed(e -> {
            soloTier.setText("—");
            flexTier.setText("—");
        });

        daemon(task);
    }

    private void applyRankCard(RankedEntry entry, Label tierLabel, Label lpLabel, Label statsLabel, ImageView emblemView) {
        if (entry.isUnranked()) {
            tierLabel.setText("Nieranked");
            lpLabel.setText("—");
            statsLabel.setText("Brak gier rankingowych");
            return;
        }
        tierLabel.setText(formatTier(entry));
        lpLabel.setText(entry.getLeaguePoints() + " LP");
        statsLabel.setText(
            "Wygrane " + entry.getWins() +
            "  ·  Przegrane " + entry.getLosses() +
            "  ·  WR " + entry.winRatePct() + "%"
        );
        emblemView.setFitWidth(68);
        emblemView.setFitHeight(68);
        emblemView.setPreserveRatio(true);
        loadEmblem(emblemView, () -> dataDragonService.getRankEmblemStream(entry.getTier()));
    }

    private void applyRankedKpi(RankedEntry solo) {
        if (!solo.isUnranked()) {
            kpiWinRate.setText(solo.winRatePct() + "%");
            kpiGames.setText(String.valueOf(solo.totalGames()));
        }
    }

    private String formatTier(RankedEntry entry) {
        if (entry.isUnranked()) return "Nieranked";
        String tier = entry.getTier().substring(0, 1).toUpperCase()
                    + entry.getTier().substring(1).toLowerCase();
        boolean hasDivision = entry.getDivision() != null && !entry.getDivision().equals("NA");
        return hasDivision ? tier + " " + entry.getDivision() : tier;
    }

    // ------------------------------------------------------------------ matches

    private void loadMatches(String puuid) {
        matchListContainer.getChildren().clear();

        Task<List<MatchSummary>> task = new Task<>() {
            @Override
            protected List<MatchSummary> call() throws Exception {
                return matchHistoryService.getRecentMatches(puuid, 10);
            }
        };

        task.setOnSucceeded(e -> {
            List<MatchSummary> matches = task.getValue();
            for (MatchSummary match : matches) {
                matchListContainer.getChildren().add(buildMatchRow(match));
            }
            applyMatchKpi(matches);
            buildTopChampions(matches);
        });

        task.setOnFailed(e -> {
            Label err = new Label("Nie udało się załadować historii meczów");
            err.getStyleClass().addAll("text-muted", "text-sm");
            matchListContainer.getChildren().add(err);
        });

        daemon(task);
    }

    private void applyMatchKpi(List<MatchSummary> matches) {
        if (matches.isEmpty()) return;

        // Average KDA
        double totalKills   = matches.stream().mapToInt(MatchSummary::getKills).sum();
        double totalDeaths  = matches.stream().mapToInt(MatchSummary::getDeaths).sum();
        double totalAssists = matches.stream().mapToInt(MatchSummary::getAssists).sum();
        double kda = totalDeaths == 0 ? totalKills + totalAssists
                                      : (totalKills + totalAssists) / totalDeaths;
        kpiKda.setText(String.format("%.2f", kda));

        // Most frequent lane
        matches.stream()
            .filter(m -> m.getLane() != null && !m.getLane().isBlank())
            .collect(Collectors.groupingBy(MatchSummary::getLane, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> {
                MatchSummary dummy = new MatchSummary();
                dummy.setLane(entry.getKey());
                String roleDisplay = dummy.getLaneDisplay();
                kpiRole.setText(roleDisplay);
                mainRoleChip.setText(roleDisplay);
            });
    }

    private void buildTopChampions(List<MatchSummary> matches) {
        // Group by champion, sort by game count descending, take top 5
        Map<String, List<MatchSummary>> byChamp = matches.stream()
            .filter(m -> !"Unknown".equals(m.getChampionName()))
            .collect(Collectors.groupingBy(MatchSummary::getChampionName));

        List<Map.Entry<String, List<MatchSummary>>> sorted = byChamp.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .limit(5)
            .toList();

        topChampionsContainer.getChildren().clear();
        boolean first = true;
        for (Map.Entry<String, List<MatchSummary>> entry : sorted) {
            if (!first) {
                Region divider = new Region();
                divider.getStyleClass().add("divider");
                topChampionsContainer.getChildren().add(divider);
            }
            first = false;
            topChampionsContainer.getChildren().add(buildChampRow(entry.getKey(), entry.getValue()));
        }
    }

    private Node buildChampRow(String champName, List<MatchSummary> games) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        // Champion icon
        ImageView icon = new ImageView();
        icon.setFitWidth(48);
        icon.setFitHeight(48);
        icon.setPreserveRatio(false);
        icon.setSmooth(true);
        Rectangle clip = new Rectangle(48, 48);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        icon.setClip(clip);
        loadImage(icon, () -> dataDragonService.getChampionImageStream(champName));

        // Name + stats
        int wins   = (int) games.stream().filter(MatchSummary::isWin).count();
        int total  = games.size();
        int losses = total - wins;
        double avgKills   = games.stream().mapToInt(MatchSummary::getKills).average().orElse(0);
        double avgDeaths  = games.stream().mapToInt(MatchSummary::getDeaths).average().orElse(0);
        double avgAssists = games.stream().mapToInt(MatchSummary::getAssists).average().orElse(0);
        double kda = avgDeaths == 0 ? avgKills + avgAssists : (avgKills + avgAssists) / avgDeaths;

        VBox info = new VBox();
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(champName);
        nameLabel.getStyleClass().addAll("text-bold", "text-lg");
        Label statsLabel = new Label(total + " gier  ·  KDA " + String.format("%.2f", kda));
        statsLabel.getStyleClass().addAll("text-muted", "text-sm");
        info.getChildren().addAll(nameLabel, statsLabel);

        // Win rate
        int wr = total == 0 ? 0 : (int) Math.round(wins * 100.0 / total);
        VBox wrBox = new VBox();
        wrBox.setAlignment(Pos.CENTER_RIGHT);
        Label wrLabel = new Label(wr + "%");
        String wrStyle = wr >= 55 ? "text-success" : wr >= 45 ? "text-gold" : "text-danger";
        wrLabel.getStyleClass().addAll(wrStyle, "text-bold", "text-lg");
        Label wrTag = new Label("WIN RATE");
        wrTag.getStyleClass().add("kpi-label");
        wrBox.getChildren().addAll(wrLabel, wrTag);

        row.getChildren().addAll(icon, info, wrBox);
        return row;
    }

    private Node buildMatchRow(MatchSummary match) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("match-row", match.isWin() ? "match-row-win" : "match-row-loss");

        // WIN / LOSS chip
        Label chip = new Label(match.isWin() ? "WIN" : "LOSS");
        chip.getStyleClass().add(match.isWin() ? "win-chip" : "loss-chip");

        // Champion icon (44×44, rounded corners)
        ImageView champIcon = new ImageView();
        champIcon.setFitWidth(44);
        champIcon.setFitHeight(44);
        champIcon.setPreserveRatio(false);
        champIcon.setSmooth(true);
        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        champIcon.setClip(clip);
        if (!match.getChampionName().equals("Unknown")) {
            loadImage(champIcon, () -> dataDragonService.getChampionImageStream(match.getChampionName()));
        }

        // Champion name + queue · time
        VBox champInfo = new VBox(3);
        HBox.setHgrow(champInfo, Priority.ALWAYS);
        Label champLabel = new Label(match.getChampionName());
        champLabel.getStyleClass().addAll("text-bold", "text-gold");
        Label queueTime = new Label(match.getQueueName() + " · " + match.getFormattedTimeAgo());
        queueTime.getStyleClass().addAll("text-muted", "text-sm");
        champInfo.getChildren().addAll(champLabel, queueTime);

        // K/D/A
        VBox kdaBox = new VBox(2);
        kdaBox.setAlignment(Pos.CENTER);
        Label kdaLabel = new Label(match.getKdaString());
        kdaLabel.getStyleClass().add("text-bold");
        Label kdaRatio = new Label(match.getFormattedKda());
        kdaRatio.getStyleClass().addAll("text-muted", "text-sm");
        kdaBox.getChildren().addAll(kdaLabel, kdaRatio);

        // Duration + lane
        VBox durationBox = new VBox(2);
        durationBox.setAlignment(Pos.CENTER_RIGHT);
        durationBox.setPrefWidth(80);
        Label durationLabel = new Label(match.getFormattedDuration());
        durationLabel.getStyleClass().add("text-muted");
        Label laneLabel = new Label(match.getLaneDisplay());
        laneLabel.getStyleClass().addAll("text-muted", "text-sm");
        durationBox.getChildren().addAll(durationLabel, laneLabel);

        row.getChildren().addAll(chip, champIcon, champInfo, kdaBox, durationBox);
        return row;
    }

    private void updateLastUpdateLabel() {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        lastUpdateLabel.setText("Ostatnia aktualizacja: " + time);
    }

    // ------------------------------------------------------------------ helpers

    @FunctionalInterface
    private interface ImageFetcher {
        InputStream fetch() throws Exception;
    }

    private void loadImage(ImageView target, ImageFetcher fetcher) {
        // Capture dimensions on FX thread before spawning background task
        double w = target.getFitWidth();
        double h = target.getFitHeight();
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                InputStream is = fetcher.fetch();
                // Resize at decode time — more reliable than relying on ImageView scaling
                return (w > 0 && h > 0) ? new Image(is, w, h, false, true) : new Image(is);
            }
        };
        task.setOnSucceeded(e -> target.setImage(task.getValue()));
        daemon(task);
    }

    // Rank emblem PNGs place a small crest in the center of a large transparent
    // canvas, with canvas/crest sizes varying per tier. Load at natural size,
    // detect the non-transparent bounding box, and use it as the viewport so the
    // crest fills the frame uniformly regardless of tier.
    private void loadEmblem(ImageView target, ImageFetcher fetcher) {
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                InputStream is = fetcher.fetch();
                return new Image(is); // natural size — viewport coords must be in source space
            }
        };
        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            Rectangle2D box = nonTransparentBounds(img);
            if (box != null) target.setViewport(box);
            target.setImage(img);
        });
        daemon(task);
    }

    private Rectangle2D nonTransparentBounds(Image img) {
        PixelReader reader = img.getPixelReader();
        if (reader == null) return null;
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int step = Math.max(1, Math.min(w, h) / 256); // sample for speed on big canvases
        int alphaThreshold = 20;
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int alpha = (reader.getArgb(x, y) >>> 24) & 0xFF;
                if (alpha > alphaThreshold) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return null; // fully transparent

        // Pad slightly so the crest isn't clipped at the edges.
        int padX = (int) ((maxX - minX) * 0.06);
        int padY = (int) ((maxY - minY) * 0.06);
        minX = Math.max(0, minX - padX);
        minY = Math.max(0, minY - padY);
        maxX = Math.min(w - 1, maxX + padX);
        maxY = Math.min(h - 1, maxY + padY);

        // Make the viewport square and centered so a preserveRatio fit doesn't distort.
        double boxW = maxX - minX + 1;
        double boxH = maxY - minY + 1;
        double side = Math.max(boxW, boxH);
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double vx = Math.max(0, Math.min(cx - side / 2.0, w - side));
        double vy = Math.max(0, Math.min(cy - side / 2.0, h - side));
        side = Math.min(side, Math.min(w - vx, h - vy));
        return new Rectangle2D(vx, vy, side, side);
    }

    private void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
