package com.yuhyfe.loldraftanalyzer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchUtilsTest {

    // ---- normalizeLane() ---------------------------------------------------

    @Test
    void normalizeLane_bottomDuoSupport_returnsUtility() {
        assertEquals("UTILITY", MatchUtils.normalizeLane("BOTTOM", "DUO_SUPPORT"));
    }

    @Test
    void normalizeLane_bottomUtility_returnsUtility() {
        assertEquals("UTILITY", MatchUtils.normalizeLane("BOTTOM", "UTILITY"));
    }

    @Test
    void normalizeLane_bottomDuoCarry_returnsBottom() {
        assertEquals("BOTTOM", MatchUtils.normalizeLane("BOTTOM", "DUO_CARRY"));
    }

    @Test
    void normalizeLane_nullLane_returnsNone() {
        assertEquals("NONE", MatchUtils.normalizeLane(null, "DUO_SUPPORT"));
    }

    @Test
    void normalizeLane_jungle_passesThrough() {
        assertEquals("JUNGLE", MatchUtils.normalizeLane("JUNGLE", null));
    }

    // ---- laneLabel() -------------------------------------------------------

    @Test
    void laneLabel_top() {
        assertEquals("Top", MatchUtils.laneLabel("TOP"));
    }

    @Test
    void laneLabel_utility_returnsSupport() {
        assertEquals("Support", MatchUtils.laneLabel("UTILITY"));
    }

    @Test
    void laneLabel_middle() {
        assertEquals("Mid", MatchUtils.laneLabel("MIDDLE"));
    }

    @Test
    void laneLabel_mid_shorthand() {
        assertEquals("Mid", MatchUtils.laneLabel("MID"));
    }

    @Test
    void laneLabel_unknown_returnsEmpty() {
        assertEquals("", MatchUtils.laneLabel("NONE"));
    }

    // ---- queueName() -------------------------------------------------------

    @Test
    void queueName_soloDuo() {
        assertEquals("Solo/Duo", MatchUtils.queueName(420));
    }

    @Test
    void queueName_flex() {
        assertEquals("Flex", MatchUtils.queueName(440));
    }

    @Test
    void queueName_aram() {
        assertEquals("ARAM", MatchUtils.queueName(450));
    }

    @Test
    void queueName_normal_draft() {
        assertEquals("Normal Draft", MatchUtils.queueName(400));
    }

    @Test
    void queueName_unknown_containsId() {
        assertTrue(MatchUtils.queueName(9999).contains("9999"));
    }

    // ---- formatDuration() --------------------------------------------------

    @Test
    void formatDuration_roundMinutes() {
        assertEquals("30:00", MatchUtils.formatDuration(1800));
    }

    @Test
    void formatDuration_paddedSeconds() {
        assertEquals("5:09", MatchUtils.formatDuration(309));
    }

    @Test
    void formatDuration_zero() {
        assertEquals("0:00", MatchUtils.formatDuration(0));
    }

    // ---- timeAgo() ---------------------------------------------------------

    @Test
    void timeAgo_zeroEpoch_returnsEmpty() {
        assertEquals("", MatchUtils.timeAgo(0));
    }

    @Test
    void timeAgo_45minutesAgo_showsMinutes() {
        long ts = System.currentTimeMillis() - 45 * 60_000L;
        assertTrue(MatchUtils.timeAgo(ts).contains("min temu"));
    }

    @Test
    void timeAgo_5hoursAgo_showsHours() {
        long ts = System.currentTimeMillis() - 5 * 3600_000L;
        assertTrue(MatchUtils.timeAgo(ts).contains("godz. temu"));
    }

    @Test
    void timeAgo_yesterday() {
        long ts = System.currentTimeMillis() - 25 * 3600_000L;
        assertEquals("wczoraj", MatchUtils.timeAgo(ts));
    }

    @Test
    void timeAgo_5daysAgo_showsDays() {
        long ts = System.currentTimeMillis() - 5L * 24 * 3600_000L;
        assertTrue(MatchUtils.timeAgo(ts).contains("dni temu"));
    }

    @Test
    void timeAgo_2monthsAgo_showsMonths() {
        long ts = System.currentTimeMillis() - 65L * 24 * 3600_000L;
        assertTrue(MatchUtils.timeAgo(ts).contains("mies. temu"));
    }
}
