package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.ranked.RankedStats;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.util.logging.Logger;

public class RankedService {

    private static final Logger LOG  = AppLogger.get(RankedService.class);
    private static final Gson   GSON = new Gson();

    public RankedStats getCurrentRankedStats() throws Exception {
        LOG.fine("fetching current-ranked-stats");
        String json = LcuConnector.getInstance().get("/lol-ranked/v1/current-ranked-stats");
        return GSON.fromJson(json, RankedStats.class);
    }
}
