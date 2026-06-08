package com.yuhyfe.loldraftanalyzer.model.champion;

import java.util.HashMap;
import java.util.Map;

public class PersonalChampionStats {

    private int championId;
    private String championName;

    private int games;
    private int wins;
    private double totalKills;
    private double totalDeaths;
    private double totalAssists;

    private final Map<String, Integer> laneCounts = new HashMap<>();

    public void addGame(boolean win, int kills, int deaths, int assists, String lane) {
        games++;
        if (win) wins++;
        totalKills   += kills;
        totalDeaths  += deaths;
        totalAssists += assists;
        if (lane != null && !lane.isBlank() && !"NONE".equals(lane)) {
            laneCounts.merge(lane, 1, Integer::sum);
        }
    }

    public double getWinRate() {
        return games == 0 ? 0 : wins * 100.0 / games;
    }

    public int getLosses() { return games - wins; }

    public double getAvgKills()   { return games == 0 ? 0 : totalKills   / games; }
    public double getAvgDeaths()  { return games == 0 ? 0 : totalDeaths  / games; }
    public double getAvgAssists() { return games == 0 ? 0 : totalAssists / games; }

    public double getKda() {
        double d = totalDeaths / Math.max(games, 1);
        return d == 0 ? (totalKills + totalAssists) / Math.max(games, 1)
                      : (totalKills + totalAssists) / (totalDeaths);
    }

    public String getMostPlayedLane() {
        return laneCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NONE");
    }

    public String getLaneFilterRole() {
        return switch (getMostPlayedLane()) {
            case "TOP"     -> "TOP";
            case "JUNGLE"  -> "JUNGLE";
            case "MIDDLE"  -> "MID";
            case "BOTTOM"  -> "ADC";
            case "UTILITY" -> "SUPPORT";
            default        -> "ALL";
        };
    }

    public String getLaneLabel() {
        return switch (getMostPlayedLane()) {
            case "TOP"     -> "TOP";
            case "JUNGLE"  -> "JG";
            case "MIDDLE"  -> "MID";
            case "BOTTOM"  -> "ADC";
            case "UTILITY" -> "SUP";
            default        -> "—";
        };
    }

    public String getRoleCssClass() {
        return switch (getMostPlayedLane()) {
            case "TOP"     -> "top";
            case "JUNGLE"  -> "jungle";
            case "MIDDLE"  -> "mid";
            case "BOTTOM"  -> "adc";
            case "UTILITY" -> "sup";
            default        -> "jungle";
        };
    }

    public int getChampionId()             { return championId; }
    public void setChampionId(int v)       { this.championId = v; }
    public String getChampionName()        { return championName; }
    public void setChampionName(String v)  { this.championName = v; }
    public int getGames()                  { return games; }
    public int getWins()                   { return wins; }
}
