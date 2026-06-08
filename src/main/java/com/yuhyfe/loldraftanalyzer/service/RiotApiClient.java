package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yuhyfe.loldraftanalyzer.AppSettings;
import com.yuhyfe.loldraftanalyzer.model.ranked.LeagueEntry;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotMatch;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class RiotApiClient {

    private static final Logger     LOG          = AppLogger.get(RiotApiClient.class);
    private static final Gson       GSON         = new Gson();
    private static final HttpClient HTTP         = HttpClient.newHttpClient();
    private static final int        WINDOW_LIMIT = 90;
    private static final long       WINDOW_MS    = 120_000L;
    private static final int        MAX_RESOLVE_CALLS = 15;

    private static final Map<String, String[]> NAME_CACHE = new ConcurrentHashMap<>();

    private final Deque<Long> window = new ArrayDeque<>();

    public List<String> getFeaturedGamePuuids(int limit) throws Exception {
        String url = "https://" + platform() + ".api.riotgames.com/lol/spectator/v5/featured-games";
        String body = get(url);
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray games = root.has("gameList") ? root.getAsJsonArray("gameList") : null;

        List<String> puuids = new ArrayList<>();
        if (games == null || games.isEmpty()) {
            LOG.warning("featured-games: empty gameList");
            return puuids;
        }

        outer:
        for (JsonElement gameEl : games) {
            JsonArray participants = gameEl.getAsJsonObject().has("participants")
                    ? gameEl.getAsJsonObject().getAsJsonArray("participants") : null;
            if (participants == null) continue;
            for (JsonElement p : participants) {
                JsonObject po = p.getAsJsonObject();
                if (!po.has("puuid") || po.get("puuid").isJsonNull()) continue;
                String puuid = po.get("puuid").getAsString();
                if (!puuid.isBlank() && !puuid.equals("0")) {
                    puuids.add(puuid);
                    if (puuids.size() >= limit) break outer;
                }
            }
        }
        LOG.info("featured-games: got " + puuids.size() + " PUUIDs");
        return puuids;
    }

    public List<String> getLeagueEntryPuuids(String tier, String division, int count) throws Exception {
        String url = "https://" + platform() + ".api.riotgames.com"
                + "/lol/league/v4/entries/RANKED_SOLO_5x5/" + tier + "/" + division + "?page=1";
        String body = get(url);
        JsonArray entries = JsonParser.parseString(body).getAsJsonArray();
        LOG.info("league entries: " + entries.size() + " results");

        List<String> puuids = new ArrayList<>();
        int fetched = 0;
        for (JsonElement entry : entries) {
            if (fetched >= count) break;
            JsonObject obj = entry.getAsJsonObject();

            if (obj.has("puuid") && !obj.get("puuid").isJsonNull()) {
                String puuid = obj.get("puuid").getAsString();
                if (!puuid.isBlank()) {
                    puuids.add(puuid);
                    fetched++;
                    continue;
                }
            }

            if (!obj.has("summonerId") || obj.get("summonerId").isJsonNull()) continue;
            String summonerId = obj.get("summonerId").getAsString();
            try {
                String puuid = getPuuidBySummonerId(summonerId);
                if (!puuid.isBlank()) {
                    puuids.add(puuid);
                    fetched++;
                }
            } catch (Exception e) {
                LOG.warning("summoner lookup failed for " + summonerId + ": " + e.getMessage());
            }
        }
        LOG.info("league entries resolved " + puuids.size() + " PUUIDs");
        return puuids;
    }

    private String getPuuidBySummonerId(String summonerId) throws Exception {
        String url = "https://" + platform() + ".api.riotgames.com/lol/summoner/v4/summoners/" + summonerId;
        JsonObject obj = JsonParser.parseString(get(url)).getAsJsonObject();
        return obj.has("puuid") ? obj.get("puuid").getAsString() : "";
    }

    public List<LeagueEntry> getLeagueTopEntries(String queue, String tier) throws Exception {
        String path = switch (tier.toUpperCase()) {
            case "CHALLENGER"  -> "/lol/league/v4/challengerleagues/by-queue/"  + queue;
            case "GRANDMASTER" -> "/lol/league/v4/grandmasterleagues/by-queue/" + queue;
            case "MASTER"      -> "/lol/league/v4/masterleagues/by-queue/"      + queue;
            default -> throw new IllegalArgumentException("Unsupported tier: " + tier);
        };
        String body = get("https://" + platform() + ".api.riotgames.com" + path);
        JsonObject root   = JsonParser.parseString(body).getAsJsonObject();
        JsonArray entries = root.has("entries") ? root.getAsJsonArray("entries") : new JsonArray();

        List<LeagueEntry> result = new ArrayList<>();
        for (JsonElement el : entries) {
            JsonObject obj = el.getAsJsonObject();
            LeagueEntry e = new LeagueEntry();
            e.setSummonerName(strOr(obj, "summonerName", ""));
            e.setPuuid(strOr(obj, "puuid", ""));
            e.setLeaguePoints(obj.has("leaguePoints") ? obj.get("leaguePoints").getAsInt() : 0);
            e.setWins(obj.has("wins")   ? obj.get("wins").getAsInt()   : 0);
            e.setLosses(obj.has("losses") ? obj.get("losses").getAsInt() : 0);
            e.setTier(tier);
            result.add(e);
        }
        result.sort(Comparator.comparingInt(LeagueEntry::getLeaguePoints).reversed());
        return result;
    }

    public void resolveAccountNames(List<LeagueEntry> entries, int limit) {
        int apiCalls = 0;
        int count    = Math.min(entries.size(), limit);
        for (int i = 0; i < count; i++) {
            LeagueEntry e = entries.get(i);
            if (e.getPuuid().isBlank()) continue;

            String[] cached = NAME_CACHE.get(e.getPuuid());
            if (cached != null) {
                e.setGameName(cached[0]);
                e.setTagLine(cached[1]);
                e.setProfileIconId(cached[2].isEmpty() ? 0 : Integer.parseInt(cached[2]));
                continue;
            }

            if (apiCalls >= MAX_RESOLVE_CALLS) continue;

            try {
                String url = "https://" + routing() + ".api.riotgames.com/riot/account/v1/accounts/by-puuid/" + e.getPuuid();
                JsonObject obj = JsonParser.parseString(get(url)).getAsJsonObject();
                e.setGameName(strOr(obj, "gameName", ""));
                e.setTagLine(strOr(obj, "tagLine", ""));
                apiCalls++;
            } catch (Exception ex) {
                LOG.warning("account resolve failed: " + ex.getMessage());
                apiCalls++;
            }

            if (!e.getPuuid().isBlank() && apiCalls < MAX_RESOLVE_CALLS) {
                try {
                    String url = "https://" + platform() + ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + e.getPuuid();
                    JsonObject obj = JsonParser.parseString(get(url)).getAsJsonObject();
                    if (obj.has("profileIconId") && !obj.get("profileIconId").isJsonNull()) {
                        e.setProfileIconId(obj.get("profileIconId").getAsInt());
                    }
                    apiCalls++;
                } catch (Exception ex) {
                    LOG.warning("summoner resolve failed: " + ex.getMessage());
                    apiCalls++;
                }
            }

            NAME_CACHE.put(e.getPuuid(), new String[]{
                e.getGameName(),
                e.getTagLine(),
                String.valueOf(e.getProfileIconId())
            });
        }
    }

    public List<String> getMatchIds(String puuid, int count) throws Exception {
        String url = "https://" + routing() + ".api.riotgames.com/lol/match/v5/matches/by-puuid/"
                + puuid + "/ids?start=0&count=" + count + "&type=ranked";
        return Arrays.asList(GSON.fromJson(get(url), String[].class));
    }

    public RiotMatch getMatch(String matchId) throws Exception {
        String url = "https://" + routing() + ".api.riotgames.com/lol/match/v5/matches/" + matchId;
        return GSON.fromJson(get(url), RiotMatch.class);
    }

    private static String strOr(JsonObject obj, String key, String def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : def;
    }

    private String routing()  { return AppSettings.get().getRoutingHost(); }
    private String platform() { return AppSettings.get().getPlatformHost(); }
    private String apiKey()   { return AppSettings.get().getRiotApiKey(); }

    private String get(String url) throws Exception {
        if (apiKey().isBlank()) {
            throw new IllegalStateException("Brak klucza Riot API. Ustaw go w Ustawieniach.");
        }
        LOG.fine("GET " + url);
        throttle();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Riot-Token", apiKey())
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.fine("-> " + response.statusCode());

        if (response.statusCode() == 429) {
            LOG.warning("rate limited, retrying after 5s");
            Thread.sleep(5_000);
            throttle();
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " | " + url + " | " + response.body());
        }
        return response.body();
    }

    private synchronized void throttle() throws InterruptedException {
        long now = System.currentTimeMillis();
        while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MS)
            window.pollFirst();
        if (window.size() >= WINDOW_LIMIT) {
            long waitMs = WINDOW_MS - (now - window.peekFirst()) + 500;
            if (waitMs > 0) {
                LOG.fine("throttle: waiting " + waitMs + "ms");
                Thread.sleep(waitMs);
            }
            now = System.currentTimeMillis();
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MS)
                window.pollFirst();
        }
        window.addLast(System.currentTimeMillis());
    }
}
