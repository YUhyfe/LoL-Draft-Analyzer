package com.yuhyfe.loldraftanalyzer.service;

import com.google.gson.Gson;
import com.yuhyfe.loldraftanalyzer.AppSettings;
import com.yuhyfe.loldraftanalyzer.lcu.LcuConnector;
import com.yuhyfe.loldraftanalyzer.model.match.Ban;
import com.yuhyfe.loldraftanalyzer.model.match.Game;
import com.yuhyfe.loldraftanalyzer.model.match.MatchHistory;
import com.yuhyfe.loldraftanalyzer.model.match.Participant;
import com.yuhyfe.loldraftanalyzer.model.match.TeamStats;
import com.yuhyfe.loldraftanalyzer.model.meta.ChampFrequency;
import com.yuhyfe.loldraftanalyzer.model.meta.MetaAggregation;
import com.yuhyfe.loldraftanalyzer.model.meta.MetaCache;
import com.yuhyfe.loldraftanalyzer.model.riot.RiotMatch;

import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MetaService {

    private static final Logger LOG  = AppLogger.get(MetaService.class);
    private static final Gson   GSON = new Gson();

    private static final int  SEED_PLAYERS      = 5;
    private static final int  MATCHES_PER_PLAYER = 20;
    private static final int  MAX_NEW_FETCHES   = 85;

    private static final int  LCU_FETCH_COUNT   = 100;
    private static final int  MIN_GAMES_FOR_WR  = 5;
    private static final int  TOP_N             = 10;

    private final DataDragonService dataDragonService;
    private final MetaCacheService  cacheService;
    private final RiotApiClient     riotClient = new RiotApiClient();

    public MetaService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
        this.cacheService      = new MetaCacheService();
    }

    public MetaAggregation aggregate(String lcuPuuid, Consumer<String> onStatus) throws Exception {
        AppSettings settings = AppSettings.get();
        boolean hasKey = settings.hasApiKey();
        LOG.info("hasApiKey=" + hasKey
                + "  keyLen=" + settings.getRiotApiKey().length()
                + "  region=[" + settings.getRegion() + "]"
                + "  platform=" + settings.getPlatformHost()
                + "  routing=" + settings.getRoutingHost());

        if (hasKey) {
            return aggregateFromRiotApi(onStatus);
        } else {
            onStatus.accept("Brak klucza Riot API — pobieranie danych z LCU…");
            return aggregateFromLcu(lcuPuuid, onStatus);
        }
    }

    private MetaAggregation aggregateFromRiotApi(Consumer<String> onStatus) throws Exception {
        Map<Integer, String> idToName = dataDragonService.getChampionIdToName();
        String patch              = dataDragonService.getLatestVersion();
        MetaCache cache           = cacheService.getCache();

        onStatus.accept("Pobieranie losowych graczy z serwera…");
        List<String> seedPuuids = new ArrayList<>();

        try {
            seedPuuids = riotClient.getFeaturedGamePuuids(SEED_PLAYERS);
            if (!seedPuuids.isEmpty())
                onStatus.accept("Znaleziono " + seedPuuids.size() + " graczy w aktywnych grach");
        } catch (Exception e) {
            LOG.warning("featured-games failed: " + e.getMessage());
        }

        if (seedPuuids.isEmpty()) {
            onStatus.accept("Featured games niedostepne — probuje losowych graczy z drabinki rankingowej…");
            try {
                seedPuuids = riotClient.getLeagueEntryPuuids("GOLD", "IV", SEED_PLAYERS);
            } catch (Exception e) {
                LOG.warning("league entries failed: " + e.getMessage());
                onStatus.accept("Blad: " + e.getMessage());
            }
        }

        if (seedPuuids.isEmpty()) {
            onStatus.accept("Nie udalo sie pobrac graczy. Sprawdz klucz API i region w Ustawieniach.");
            if (cache.getTotalGamesProcessed() > 0) {
                onStatus.accept("Pokazuje dane z cache (" + cache.getTotalGamesProcessed() + " meczow)");
                return buildFromCache(cache, patch);
            }
            return empty("Brak danych. Sprawdz klucz API i region w Ustawieniach.");
        }

        onStatus.accept("Pobieranie historii meczow dla " + seedPuuids.size() + " graczy…");
        Set<String> allMatchIds = new HashSet<>();
        for (String puuid : seedPuuids) {
            try {
                List<String> ids = riotClient.getMatchIds(puuid, MATCHES_PER_PLAYER);
                allMatchIds.addAll(ids);
            } catch (Exception e) {
                LOG.warning("skip puuid match IDs: " + e.getMessage());
            }
        }

        List<String> toFetch = allMatchIds.stream()
                .filter(id -> !cache.wasProcessed(id))
                .limit(MAX_NEW_FETCHES)
                .collect(Collectors.toList());

        if (toFetch.isEmpty()) {
            onStatus.accept("Cache aktualny — " + cache.getTotalGamesProcessed() + " meczow w bazie");
        } else {
            int total   = toFetch.size();
            int fetched = 0;
            for (String matchId : toFetch) {
                try {
                    RiotMatch match = riotClient.getMatch(matchId);
                    cacheService.merge(matchId, match, idToName, patch);
                    fetched++;
                    onStatus.accept("Pobieranie: " + fetched + " / " + total
                            + "  (cache: " + cache.getTotalGamesProcessed() + " meczow)");
                } catch (Exception e) {
                    LOG.warning("skip match " + matchId + ": " + e.getMessage());
                }
            }
            cacheService.save();
            onStatus.accept("Gotowe — cache: " + cache.getTotalGamesProcessed() + " meczow");
        }

        return buildFromCache(cache, patch);
    }

    private MetaAggregation aggregateFromLcu(String puuid,
                                              Consumer<String> onStatus) throws Exception {
        onStatus.accept("Pobieranie historii meczow z LCU…");
        String json = LcuConnector.getInstance().get(
            "/lol-match-history/v1/products/lol/" + puuid
            + "/matches?begIndex=0&endIndex=" + (LCU_FETCH_COUNT - 1)
        );

        MatchHistory history = GSON.fromJson(json, MatchHistory.class);
        if (history == null || history.getGames() == null
                || history.getGames().getGames() == null) {
            return empty("Brak historii meczow w LCU");
        }

        Map<Integer, String> champNames = dataDragonService.getChampionIdToName();
        List<Game> games                = history.getGames().getGames();
        int totalGames                  = games.size();

        Map<String, Integer> pickCounts = new HashMap<>();
        Map<String, Integer> banCounts  = new HashMap<>();
        Map<String, int[]>   wrData     = new HashMap<>();

        for (Game game : games) {
            int winningTeamId = resolveWinningTeam(game);
            if (game.getParticipants() != null) {
                for (Participant p : game.getParticipants()) {
                    String name = champNames.get(p.getChampionId());
                    if (name == null) continue;
                    pickCounts.merge(name, 1, Integer::sum);
                    int pTeam = resolveParticipantTeam(p);
                    if (winningTeamId != 0 && pTeam != 0) {
                        int[] wr = wrData.computeIfAbsent(name, k -> new int[]{0, 0});
                        if (pTeam == winningTeamId) wr[0]++;
                        wr[1]++;
                    }
                }
            }
            if (game.getTeams() != null) {
                for (TeamStats team : game.getTeams()) {
                    if (team.getBans() == null) continue;
                    for (Ban ban : team.getBans()) {
                        if (ban.getChampionId() <= 0) continue;
                        String name = champNames.get(ban.getChampionId());
                        if (name == null) continue;
                        banCounts.merge(name, 1, Integer::sum);
                    }
                }
            }
        }

        int totalSlots = Math.max(totalGames * 10, 1);
        List<ChampFrequency> mostPicked = toFreqList(pickCounts, totalSlots, TOP_N);
        List<ChampFrequency> mostBanned = toFreqList(banCounts,  totalSlots, TOP_N);
        List<ChampFrequency> bestWr     = buildWrList(wrData);

        String patch = dataDragonService.getLatestVersion();
        MetaAggregation agg = new MetaAggregation(mostPicked, mostBanned, bestWr, totalGames, patch);
        agg.setLastUpdated("LCU (tylko Twoje mecze)");
        return agg;
    }

    private MetaAggregation buildFromCache(MetaCache cache, String patch) {
        int total = Math.max(cache.getTotalGamesProcessed(), 1);
        int totalSlots = Math.max(total * 10, 1);

        List<ChampFrequency> mostPicked = toFreqList(cache.getPicks(), totalSlots, TOP_N);
        List<ChampFrequency> mostBanned = toFreqList(cache.getBans(),  totalSlots, TOP_N);

        List<ChampFrequency> bestWr = cache.getGamesPerChamp().entrySet().stream()
                .filter(e -> e.getValue() >= MIN_GAMES_FOR_WR)
                .map(e -> {
                    String champ = e.getKey();
                    int    plays = e.getValue();
                    int    wins  = cache.getWins().getOrDefault(champ, 0);
                    return new ChampFrequency(champ, plays, wins * 100.0 / plays);
                })
                .sorted(Comparator.comparingDouble(ChampFrequency::getPct).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        String patchLabel = cache.getPatchVersion().isBlank() ? patch : cache.getPatchVersion();
        MetaAggregation agg = new MetaAggregation(
                mostPicked, mostBanned, bestWr,
                cache.getTotalGamesProcessed(), patchLabel);
        agg.setLastUpdated(cache.getLastUpdated());
        agg.setCachedGames(cache.getProcessedMatchIds().size());
        return agg;
    }

    private List<ChampFrequency> buildWrList(Map<String, int[]> wrData) {
        return wrData.entrySet().stream()
                .filter(e -> e.getValue()[1] >= MIN_GAMES_FOR_WR)
                .map(e -> new ChampFrequency(e.getKey(), e.getValue()[1],
                        e.getValue()[0] * 100.0 / e.getValue()[1]))
                .sorted(Comparator.comparingDouble(ChampFrequency::getPct).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    private static int resolveWinningTeam(Game game) {
        if (game.getTeams() == null) return 0;
        for (TeamStats t : game.getTeams())
            if ("Win".equalsIgnoreCase(t.getWin())) return t.getTeamId();
        return 0;
    }

    private static int resolveParticipantTeam(Participant p) {
        if (p.getTeamId() == 100 || p.getTeamId() == 200) return p.getTeamId();
        if (p.getParticipantId() >= 1 && p.getParticipantId() <= 5)  return 100;
        if (p.getParticipantId() >= 6 && p.getParticipantId() <= 10) return 200;
        return 0;
    }

    private static List<ChampFrequency> toFreqList(Map<String, Integer> counts,
                                                    int total, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ChampFrequency(e.getKey(), e.getValue(),
                        e.getValue() * 100.0 / total))
                .collect(Collectors.toList());
    }

    private static MetaAggregation empty(String reason) {
        MetaAggregation a = new MetaAggregation(List.of(), List.of(), List.of(), 0, "—");
        a.setLastUpdated(reason);
        return a;
    }
}
