package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.match.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchHistoryService {

    private static final Gson GSON = new Gson();
    private final DataDragonService dataDragonService;

    public MatchHistoryService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    public List<MatchSummary> getRecentMatches(String puuid, int count) throws Exception {
        String json = LcuConnector.getInstance().get(
            "/lol-match-history/v1/products/lol/" + puuid + "/matches?begIndex=0&endIndex=" + (count - 1)
        );
        MatchHistory history = GSON.fromJson(json, MatchHistory.class);
        Map<Integer, String> champMap = dataDragonService.getChampionIdToName();

        List<MatchSummary> result = new ArrayList<>();
        for (Game game : history.getGames().getGames()) {
            // LCU returns only our participant — take first element
            Participant p = game.getParticipants().get(0);
            String champName = champMap.getOrDefault(p.getChampionId(), "Unknown");

            MatchSummary s = new MatchSummary();
            s.setChampionId(p.getChampionId());
            s.setChampionName(champName);
            s.setKills(p.getStats().getKills());
            s.setDeaths(p.getStats().getDeaths());
            s.setAssists(p.getStats().getAssists());
            s.setWin(p.getStats().isWin());
            s.setDurationSeconds(game.getGameDuration());
            s.setGameCreationMs(game.getGameCreation());
            s.setQueueId(game.getQueueId());
            s.setLane(p.getTimeline().getLane());
            result.add(s);
        }
        return result;
    }
}
