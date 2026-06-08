package com.yuhyfe.loldraftanalyzer.model.ranked;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class LeagueEntry {
    private String summonerName;
    private String puuid;
    private String gameName;
    private String tagLine;
    private int    profileIconId;
    private int    leaguePoints;
    private int    wins;
    private int    losses;
    private String tier;

    public String displayName() {
        if (gameName != null && !gameName.isBlank()) {
            return tagLine != null && !tagLine.isBlank()
                   ? gameName + " #" + tagLine
                   : gameName;
        }
        if (summonerName != null && !summonerName.isBlank()) return summonerName;
        return "Gracz";
    }

    public int totalGames() { return wins + losses; }

    public double winRatePct() {
        return totalGames() == 0 ? 0.0 : wins * 100.0 / totalGames();
    }
}
