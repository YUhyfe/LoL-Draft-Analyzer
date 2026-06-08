package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yuhyfe.loldraftanalyzer.model.meta.MetaCache;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotBan;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotMatch;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotParticipant;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotTeam;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class MetaCacheService {

    private static final Logger LOG  = AppLogger.get(MetaCacheService.class);
    private static final Gson   GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   DIR  = Path.of(System.getenv("APPDATA"), "LoLDraftAnalyzer");
    private static final Path   FILE = DIR.resolve("meta_cache.json");

    private MetaCache cache;

    public MetaCacheService() {
        cache = load();
    }

    public MetaCache getCache() { return cache; }

    public void merge(String matchId, RiotMatch match, Map<Integer, String> champIdToName, String patch) {
        if (match == null || match.getInfo() == null) return;
        if (cache.wasProcessed(matchId)) return;

        cache.markProcessed(matchId);
        cache.incrementTotalGames();
        cache.setPatchVersion(patch);

        if (match.getInfo().getParticipants() != null) {
            for (RiotParticipant p : match.getInfo().getParticipants()) {
                String name = p.getChampionName();
                if (name == null || name.isBlank()) continue;
                cache.addResult(name, p.isWin());
            }
            for (RiotParticipant p : match.getInfo().getParticipants()) {
                String name = p.getChampionName();
                if (name == null || name.isBlank()) continue;
                cache.getPicks().merge(name, 1, Integer::sum);
            }
        }

        if (match.getInfo().getTeams() != null) {
            for (RiotTeam team : match.getInfo().getTeams()) {
                if (team.getBans() == null) continue;
                for (RiotBan ban : team.getBans()) {
                    if (ban.getChampionId() <= 0) continue;
                    String name = champIdToName.get(ban.getChampionId());
                    if (name != null) cache.addBan(name);
                }
            }
        }

        cache.setLastUpdated(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    public void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(cache));
        } catch (IOException e) {
            LOG.warning("could not save cache: " + e.getMessage());
        }
    }

    private static MetaCache load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                MetaCache loaded = GSON.fromJson(json, MetaCache.class);
                if (loaded != null) return fixNulls(loaded);
            }
        } catch (IOException ignored) {}
        return new MetaCache();
    }

    private static MetaCache fixNulls(MetaCache c) {
        if (c.getProcessedMatchIds() == null) c.setProcessedMatchIds(new HashSet<>());
        if (c.getPicks()             == null) c.setPicks(new HashMap<>());
        if (c.getBans()              == null) c.setBans(new HashMap<>());
        if (c.getWins()              == null) c.setWins(new HashMap<>());
        if (c.getGamesPerChamp()     == null) c.setGamesPerChamp(new HashMap<>());
        return c;
    }
}
