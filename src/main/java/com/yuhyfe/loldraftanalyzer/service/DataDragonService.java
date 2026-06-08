package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yuhyfe.loldraftanalyzer.model.champion.ChampionEntry;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DataDragonService {

    private static final Logger     LOG     = AppLogger.get(DataDragonService.class);
    private static final String     DDRAGON = "https://ddragon.leagueoflegends.com";
    private static final String     CDRAGON = "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default";
    private static final Gson       GSON    = new Gson();
    private static final HttpClient HTTP    = HttpClient.newHttpClient();

    private String              cachedVersion;
    private Map<Integer, String> cachedChampionMap;

    public String getLatestVersion() throws Exception {
        if (cachedVersion != null) return cachedVersion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DDRAGON + "/api/versions.json"))
                .GET()
                .build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        cachedVersion = GSON.fromJson(body, String[].class)[0];
        LOG.fine("DDragon latest version: " + cachedVersion);
        return cachedVersion;
    }

    public ByteArrayInputStream getProfileIconStream(int iconId) throws Exception {
        return fetchBytes(DDRAGON + "/cdn/" + getLatestVersion() + "/img/profileicon/" + iconId + ".png");
    }

    public ByteArrayInputStream getRankEmblemStream(String tier) throws Exception {
        return fetchBytes(CDRAGON + "/ranked-emblem/emblem-" + tier.toLowerCase() + ".png");
    }

    public ByteArrayInputStream getChampionImageStream(String championName) throws Exception {
        return fetchBytes(DDRAGON + "/cdn/" + getLatestVersion() + "/img/champion/" + championName + ".png");
    }

    public List<ChampionEntry> getAllChampions() throws Exception {
        String url = DDRAGON + "/cdn/" + getLatestVersion() + "/data/en_US/champion.json";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject data = GSON.fromJson(body, JsonObject.class).getAsJsonObject("data");

        List<ChampionEntry> list = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            JsonObject c = entry.getValue().getAsJsonObject();
            ChampionEntry champ = new ChampionEntry();
            champ.setId(c.get("id").getAsString());
            champ.setName(c.get("name").getAsString());
            champ.setChampionKey(Integer.parseInt(c.get("key").getAsString()));
            List<String> tags = new ArrayList<>();
            for (JsonElement tag : c.getAsJsonArray("tags")) {
                tags.add(tag.getAsString());
            }
            champ.setTags(tags);
            list.add(champ);
        }
        list.sort(Comparator.comparing(ChampionEntry::getName));
        return list;
    }

    public Map<Integer, String> getChampionIdToName() throws Exception {
        if (cachedChampionMap != null) return cachedChampionMap;
        String url = DDRAGON + "/cdn/" + getLatestVersion() + "/data/en_US/champion.json";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject data = GSON.fromJson(body, JsonObject.class).getAsJsonObject("data");
        cachedChampionMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String key = entry.getValue().getAsJsonObject().get("key").getAsString();
            cachedChampionMap.put(Integer.parseInt(key), entry.getKey());
        }
        return cachedChampionMap;
    }

    private ByteArrayInputStream fetchBytes(String url) throws Exception {
        LOG.fine("DDragon GET " + url);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        byte[] bytes = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        return new ByteArrayInputStream(bytes);
    }
}
