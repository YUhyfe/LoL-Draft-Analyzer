package com.yuhyfe.loldraftanalyzer.model.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchSummaryTest {

    // ---- getFormattedDuration() ---------------------------------------------

    @Test
    void formattedDuration_formatsMmSs() {
        MatchSummary m = new MatchSummary();
        m.setDurationSeconds(1865); // 31 min 5 s
        assertEquals("31:05", m.getFormattedDuration());
    }

    @Test
    void formattedDuration_zeroDuration() {
        MatchSummary m = new MatchSummary();
        m.setDurationSeconds(0);
        assertEquals("0:00", m.getFormattedDuration());
    }

    // ---- getFormattedTimeAgo() ----------------------------------------------

    @Test
    void formattedTimeAgo_lessThan60Minutes_showsMinutes() {
        MatchSummary m = new MatchSummary();
        m.setGameCreationMs(System.currentTimeMillis() - 30 * 60_000L);
        String result = m.getFormattedTimeAgo();
        assertTrue(result.contains("min temu"), "Expected 'min temu' in: " + result);
    }

    @Test
    void formattedTimeAgo_lessThan24Hours_showsHours() {
        MatchSummary m = new MatchSummary();
        m.setGameCreationMs(System.currentTimeMillis() - 3 * 3600_000L);
        String result = m.getFormattedTimeAgo();
        assertTrue(result.contains("godz. temu"), "Expected 'godz. temu' in: " + result);
    }

    @Test
    void formattedTimeAgo_yesterday() {
        MatchSummary m = new MatchSummary();
        m.setGameCreationMs(System.currentTimeMillis() - 25 * 3600_000L);
        assertEquals("wczoraj", m.getFormattedTimeAgo());
    }

    @Test
    void formattedTimeAgo_moreThan30Days_showsMonths() {
        MatchSummary m = new MatchSummary();
        m.setGameCreationMs(System.currentTimeMillis() - 62L * 24 * 3600_000L); // ~2 months
        String result = m.getFormattedTimeAgo();
        assertTrue(result.contains("mies. temu"), "Expected 'mies. temu' in: " + result);
    }

    // ---- getLaneDisplay() ---------------------------------------------------

    @Test
    void laneDisplay_jungle() {
        MatchSummary m = new MatchSummary();
        m.setLane("JUNGLE");
        assertEquals("Jungle", m.getLaneDisplay());
    }

    @Test
    void laneDisplay_utility_returnsSupport() {
        MatchSummary m = new MatchSummary();
        m.setLane("UTILITY");
        assertEquals("Support", m.getLaneDisplay());
    }

    @Test
    void laneDisplay_null_returnsDash() {
        MatchSummary m = new MatchSummary();
        m.setLane(null);
        assertEquals("—", m.getLaneDisplay());
    }

    @Test
    void laneDisplay_blank_returnsDash() {
        MatchSummary m = new MatchSummary();
        m.setLane("");
        assertEquals("—", m.getLaneDisplay());
    }

    // ---- getQueueName() (via MatchUtils) ------------------------------------

    @Test
    void queueName_solo() {
        MatchSummary m = new MatchSummary();
        m.setQueueId(420);
        assertEquals("Solo/Duo", m.getQueueName());
    }

    @Test
    void queueName_aram() {
        MatchSummary m = new MatchSummary();
        m.setQueueId(450);
        assertEquals("ARAM", m.getQueueName());
    }

    // ---- getFormattedKda() --------------------------------------------------

    @Test
    void formattedKda_normalDeaths() {
        MatchSummary m = new MatchSummary();
        m.setKills(6);
        m.setDeaths(2);
        m.setAssists(10);
        // (6+10)/2 = 8.00
        assertEquals("KDA 8,00", m.getFormattedKda().replace(".", ","));
    }

    @Test
    void formattedKda_zeroDeaths_usesKillsPlusAssists() {
        MatchSummary m = new MatchSummary();
        m.setKills(5);
        m.setDeaths(0);
        m.setAssists(3);
        // deaths == 0 => ratio = kills + assists = 8
        assertTrue(m.getFormattedKda().contains("8"), m.getFormattedKda());
    }
}
