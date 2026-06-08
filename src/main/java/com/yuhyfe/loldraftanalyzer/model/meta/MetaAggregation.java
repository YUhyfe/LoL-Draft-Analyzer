package com.yuhyfe.loldraftanalyzer.model.meta;

import java.util.List;

public class MetaAggregation {
    private final List<ChampFrequency> mostPicked;
    private final List<ChampFrequency> mostBanned;
    private final List<ChampFrequency> bestWinRate;
    private final int    totalGamesAnalyzed;
    private final String patchVersion;
    private String lastUpdated = "";
    private int    cachedGames = 0;

    public MetaAggregation(List<ChampFrequency> mostPicked,
                           List<ChampFrequency> mostBanned,
                           List<ChampFrequency> bestWinRate,
                           int totalGamesAnalyzed,
                           String patchVersion) {
        this.mostPicked = mostPicked;
        this.mostBanned = mostBanned;
        this.bestWinRate = bestWinRate;
        this.totalGamesAnalyzed = totalGamesAnalyzed;
        this.patchVersion = patchVersion;
    }

    public List<ChampFrequency> getMostPicked()    { return mostPicked; }
    public List<ChampFrequency> getMostBanned()    { return mostBanned; }
    public List<ChampFrequency> getBestWinRate()   { return bestWinRate; }
    public int    getTotalGamesAnalyzed()          { return totalGamesAnalyzed; }
    public String getPatchVersion()                { return patchVersion; }
    public String getLastUpdated()                 { return lastUpdated; }
    public void   setLastUpdated(String v)         { lastUpdated = v; }
    public int    getCachedGames()                 { return cachedGames; }
    public void   setCachedGames(int v)            { cachedGames = v; }
    public boolean isFromCache()                   { return cachedGames > 0; }
}
