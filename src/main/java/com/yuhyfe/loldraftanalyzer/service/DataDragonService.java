package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class DataDragonService {

    private static final String DDRAGON  = "https://ddragon.leagueoflegends.com";
    private static final String CDRAGON  = "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private String cachedVersion;
    private Map<Integer, String> cachedChampionMap;

    public String getLatestVersion() throws Exception {
        if (cachedVersion != null) return cachedVersion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DDRAGON + "/api/versions.json"))
                .GET()
                .build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        cachedVersion = GSON.fromJson(body, String[].class)[0];
        return cachedVersion;
    }

    public ByteArrayInputStream getProfileIconStream(int iconId) throws Exception {
        String url = DDRAGON + "/cdn/" + getLatestVersion() + "/img/profileicon/" + iconId + ".png";
        return fetchBytes(url);
    }

    public ByteArrayInputStream getRankEmblemStream(String tier) throws Exception {
        // emblem-xxx.png: 256×256 transparent RGBA — full crest emblem (not just the plate frame)
        return fetchBytes(CDRAGON + "/ranked-emblem/emblem-" + tier.toLowerCase() + ".png");
    }

    public ByteArrayInputStream getChampionImageStream(String championName) throws Exception {
        return fetchBytes(DDRAGON + "/cdn/" + getLatestVersion() + "/img/champion/" + championName + ".png");
    }

    public Map<Integer, String> getChampionIdToName() throws Exception {
        if (cachedChampionMap != null) return cachedChampionMap;
        String url = DDRAGON + "/cdn/" + getLatestVersion() + "/data/en_US/champion.json";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        String body = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject data = GSON.fromJson(body, JsonObject.class).getAsJsonObject("data");
        cachedChampionMap = new HashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
            String key = entry.getValue().getAsJsonObject().get("key").getAsString();
            cachedChampionMap.put(Integer.parseInt(key), entry.getKey());
        }
        return cachedChampionMap;
    }

    private ByteArrayInputStream fetchBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        byte[] bytes = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        return new ByteArrayInputStream(bytes);
    }
}
