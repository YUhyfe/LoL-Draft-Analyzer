package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.summoner.Summoner;

public class SummonerService {

    private static final Gson GSON = new Gson();

    public Summoner getCurrentSummoner() throws Exception {
        String json = LcuConnector.getInstance().get("/lol-summoner/v1/current-summoner");
        Summoner summoner = GSON.fromJson(json, Summoner.class);

        // platformId is not in the summoner endpoint — fetch it separately
        try {
            String regionJson = LcuConnector.getInstance().get("/riotclient/region-locale");
            JsonObject obj = GSON.fromJson(regionJson, JsonObject.class);
            if (obj.has("webRegion")) {
                summoner.setPlatformId(obj.get("webRegion").getAsString());
            }
        } catch (Exception ignored) {}

        return summoner;
    }
}
