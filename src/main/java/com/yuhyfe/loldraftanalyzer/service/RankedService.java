package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.ranked.RankedStats;

public class RankedService {

    private static final Gson GSON = new Gson();

    public RankedStats getCurrentRankedStats() throws Exception {
        String json = LcuConnector.getInstance().get("/lol-ranked/v1/current-ranked-stats");
        return GSON.fromJson(json, RankedStats.class);
    }
}
