package com.yuhyfe.loldraftanalyzer.model.meta;

public class ChampFrequency {
    private final String championName;
    private final int count;
    private final double pct;

    public ChampFrequency(String championName, int count, double pct) {
        this.championName = championName;
        this.count = count;
        this.pct = pct;
    }

    public String getChampionName() { return championName; }
    public int getCount()           { return count; }
    public double getPct()          { return pct; }
}
