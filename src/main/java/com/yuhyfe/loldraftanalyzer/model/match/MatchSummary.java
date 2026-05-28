package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatchSummary {
    private int championId;
    private String championName;
    private int kills;
    private int deaths;
    private int assists;
    private boolean win;
    private long durationSeconds;
    private long gameCreationMs;
    private int queueId;
    private String lane;

    public String getFormattedDuration() {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }

    public String getFormattedTimeAgo() {
        long diffMin = (System.currentTimeMillis() - gameCreationMs) / 60_000;
        if (diffMin < 60)  return diffMin + " min temu";
        long diffH = diffMin / 60;
        if (diffH < 24)    return diffH + " godz. temu";
        long diffD = diffH / 24;
        if (diffD == 1)    return "wczoraj";
        return diffD + " dni temu";
    }

    public String getQueueName() {
        return switch (queueId) {
            case 420 -> "Solo Queue";
            case 440 -> "Ranked Flex";
            case 450 -> "ARAM";
            case 400 -> "Normal Draft";
            case 430 -> "Normal Blind";
            default  -> "Gra #" + queueId;
        };
    }

    public String getKdaString() {
        return kills + " / " + deaths + " / " + assists;
    }

    public String getFormattedKda() {
        double ratio = deaths == 0 ? (kills + assists) : (kills + assists) / (double) deaths;
        return String.format("KDA %.2f", ratio);
    }

    public String getLaneDisplay() {
        return switch (lane == null ? "" : lane) {
            case "JUNGLE"  -> "Jungle";
            case "MIDDLE"  -> "Mid";
            case "TOP"     -> "Top";
            case "BOTTOM"  -> "Bot";
            case "UTILITY" -> "Support";
            default        -> "—";
        };
    }
}
