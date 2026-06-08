package com.yuhyfe.loldraftanalyzer.model.meta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetaCache {

    private Set<String>         processedMatchIds = new HashSet<>();
    private int                 totalGamesProcessed = 0;
    private Map<String, Integer> picks    = new HashMap<>();
    private Map<String, Integer> bans     = new HashMap<>();
    private Map<String, Integer> wins     = new HashMap<>();
    private Map<String, Integer> gamesPerChamp = new HashMap<>();
    private String              lastUpdated  = "";
    private String              patchVersion = "";

    public Set<String>          getProcessedMatchIds()  { return processedMatchIds; }
    public void                 setProcessedMatchIds(Set<String> v) { processedMatchIds = v; }

    public int                  getTotalGamesProcessed() { return totalGamesProcessed; }
    public void                 setTotalGamesProcessed(int v) { totalGamesProcessed = v; }

    public Map<String, Integer> getPicks()    { return picks; }
    public void                 setPicks(Map<String, Integer> v) { picks = v; }

    public Map<String, Integer> getBans()     { return bans; }
    public void                 setBans(Map<String, Integer> v) { bans = v; }

    public Map<String, Integer> getWins()     { return wins; }
    public void                 setWins(Map<String, Integer> v) { wins = v; }

    public Map<String, Integer> getGamesPerChamp() { return gamesPerChamp; }
    public void                 setGamesPerChamp(Map<String, Integer> v) { gamesPerChamp = v; }

    public String               getLastUpdated()  { return lastUpdated == null ? "" : lastUpdated; }
    public void                 setLastUpdated(String v) { lastUpdated = v; }

    public String               getPatchVersion() { return patchVersion == null ? "" : patchVersion; }
    public void                 setPatchVersion(String v) { patchVersion = v; }

    public boolean wasProcessed(String matchId) {
        return processedMatchIds.contains(matchId);
    }

    public void markProcessed(String matchId) {
        processedMatchIds.add(matchId);
    }

    public void addPick(String champ) {
        picks.merge(champ, 1, Integer::sum);
        gamesPerChamp.merge(champ, 1, Integer::sum);
    }

    public void addBan(String champ) {
        bans.merge(champ, 1, Integer::sum);
    }

    public void addResult(String champ, boolean won) {
        gamesPerChamp.merge(champ, 1, Integer::sum);
        if (won) wins.merge(champ, 1, Integer::sum);
    }

    public void incrementTotalGames() {
        totalGamesProcessed++;
    }
}
