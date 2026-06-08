package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.match.*;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;
import com.yuhyfe.loldraftanalyzer.util.MatchUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MatchHistoryService {

    private static final Logger LOG  = AppLogger.get(MatchHistoryService.class);
    private static final Gson   GSON = new Gson();

    private final DataDragonService dataDragonService;

    public MatchHistoryService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    public List<MatchSummary> getRecentMatches(String puuid, String gameName, int count) throws Exception {
        LOG.fine("getRecentMatches puuid=" + puuid + " count=" + count);
        String json = LcuConnector.getInstance().get(
            "/lol-match-history/v1/products/lol/" + puuid + "/matches?begIndex=0&endIndex=" + (count - 1)
        );
        MatchHistory history = GSON.fromJson(json, MatchHistory.class);
        Map<Integer, String> champMap = dataDragonService.getChampionIdToName();

        List<MatchSummary> result = new ArrayList<>();
        for (Game game : history.getGames().getGames()) {
            Participant p = MatchUtils.findParticipant(game, gameName);
            if (p == null) continue;
            if (p.getStats() == null) continue;

            String lane = MatchUtils.normalizeLane(
                p.getTimeline() != null ? p.getTimeline().getLane() : null,
                p.getTimeline() != null ? p.getTimeline().getRole() : null
            );

            MatchSummary s = new MatchSummary();
            s.setChampionId(p.getChampionId());
            s.setChampionName(champMap.getOrDefault(p.getChampionId(), "Unknown"));
            s.setKills(p.getStats().getKills());
            s.setDeaths(p.getStats().getDeaths());
            s.setAssists(p.getStats().getAssists());
            s.setWin(p.getStats().isWin());
            s.setDurationSeconds(game.getGameDuration());
            s.setGameCreationMs(game.getGameCreation());
            s.setQueueId(game.getQueueId());
            s.setLane(lane);
            result.add(s);
        }
        LOG.fine("getRecentMatches -> " + result.size() + " summaries");
        return result;
    }
}
