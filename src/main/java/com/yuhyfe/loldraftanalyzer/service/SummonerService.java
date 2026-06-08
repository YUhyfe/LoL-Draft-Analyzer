package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.summoner.Summoner;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.util.logging.Logger;

public class SummonerService {

    private static final Logger LOG  = AppLogger.get(SummonerService.class);
    private static final Gson   GSON = new Gson();

    public Summoner getCurrentSummoner() throws Exception {
        String json = LcuConnector.getInstance().get("/lol-summoner/v1/current-summoner");
        Summoner summoner = GSON.fromJson(json, Summoner.class);
        LOG.fine("currentSummoner: " + summoner.getGameName() + " puuid=" + summoner.getPuuid());

        try {
            String regionJson = LcuConnector.getInstance().get("/riotclient/region-locale");
            JsonObject obj = GSON.fromJson(regionJson, JsonObject.class);
            if (obj.has("webRegion")) {
                summoner.setPlatformId(obj.get("webRegion").getAsString());
                LOG.fine("region: " + summoner.getPlatformId());
            }
        } catch (Exception e) {
            LOG.warning("could not fetch region-locale: " + e.getMessage());
        }

        return summoner;
    }
}
