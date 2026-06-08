package com.yuhyfe.loldraftanalyzer.model.ranked;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RankedEntry {
    private String queueType;
    private String tier;
    private String division;
    private int leaguePoints;
    private int wins;
    private int losses;
    private boolean isProvisional;

    public boolean isUnranked() {
        return tier == null || tier.isEmpty();
    }

    public int totalGames() {
        return wins + losses;
    }

    public int winRatePct() {
        if (totalGames() == 0) return 0;
        return (int) Math.round(wins * 100.0 / totalGames());
    }
}
