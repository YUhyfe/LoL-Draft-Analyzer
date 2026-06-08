package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.champion.PersonalChampionStats;
import com.yuhyfe.loldraftanalyzer.model.match.Game;
import com.yuhyfe.loldraftanalyzer.model.match.MatchHistory;
import com.yuhyfe.loldraftanalyzer.model.match.Participant;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;
import com.yuhyfe.loldraftanalyzer.util.MatchUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChampionStatsService {

    private static final Logger LOG        = AppLogger.get(ChampionStatsService.class);
    private static final Gson   GSON       = new Gson();
    private static final int    FETCH_COUNT = 100;

    private final DataDragonService dataDragonService;

    public ChampionStatsService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    public List<PersonalChampionStats> getChampionStats(String puuid, String gameName) throws Exception {
        LOG.fine("getChampionStats puuid=" + puuid);
        String json = LcuConnector.getInstance().get(
            "/lol-match-history/v1/products/lol/" + puuid
            + "/matches?begIndex=0&endIndex=" + (FETCH_COUNT - 1)
        );

        MatchHistory history = GSON.fromJson(json, MatchHistory.class);
        if (history == null || history.getGames() == null || history.getGames().getGames() == null) {
            return List.of();
        }

        Map<Integer, String> champNames = dataDragonService.getChampionIdToName();
        Map<Integer, PersonalChampionStats> statsMap = new HashMap<>();

        for (Game game : history.getGames().getGames()) {
            if (game.getParticipants() == null || game.getParticipants().isEmpty()) continue;

            Participant p = MatchUtils.findParticipant(game, gameName);
            if (p == null || p.getStats() == null) continue;

            int champId = p.getChampionId();
            String lane = MatchUtils.normalizeLane(
                p.getTimeline() != null ? p.getTimeline().getLane() : null,
                p.getTimeline() != null ? p.getTimeline().getRole() : null
            );

            PersonalChampionStats stats = statsMap.computeIfAbsent(champId, k -> {
                PersonalChampionStats s = new PersonalChampionStats();
                s.setChampionId(champId);
                s.setChampionName(champNames.getOrDefault(champId, "Unknown"));
                return s;
            });

            stats.addGame(
                p.getStats().isWin(),
                p.getStats().getKills(),
                p.getStats().getDeaths(),
                p.getStats().getAssists(),
                lane
            );
        }

        List<PersonalChampionStats> result = statsMap.values().stream()
                .filter(s -> !"Unknown".equals(s.getChampionName()))
                .sorted(Comparator.comparingInt(PersonalChampionStats::getGames).reversed())
                .collect(Collectors.toList());

        LOG.fine("getChampionStats -> " + result.size() + " champions");
        return result;
    }
}
