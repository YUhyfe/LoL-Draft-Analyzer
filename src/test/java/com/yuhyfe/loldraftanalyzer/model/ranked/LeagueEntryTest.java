package com.yuhyfe.loldraftanalyzer.model.ranked;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeagueEntryTest {

    // ---- displayName() -------------------------------------------------------

    @Test
    void displayName_withGameNameAndTag_returnsGameNameHashTag() {
        LeagueEntry e = new LeagueEntry();
        e.setGameName("Faker");
        e.setTagLine("T1");
        assertEquals("Faker #T1", e.displayName());
    }

    @Test
    void displayName_withGameNameNoTag_returnsGameNameOnly() {
        LeagueEntry e = new LeagueEntry();
        e.setGameName("Faker");
        e.setTagLine(null);
        assertEquals("Faker", e.displayName());
    }

    @Test
    void displayName_withGameNameBlankTag_returnsGameNameOnly() {
        LeagueEntry e = new LeagueEntry();
        e.setGameName("Faker");
        e.setTagLine("   ");
        assertEquals("Faker", e.displayName());
    }

    @Test
    void displayName_fallbackToSummonerName() {
        LeagueEntry e = new LeagueEntry();
        e.setGameName("");
        e.setSummonerName("OldFaker");
        assertEquals("OldFaker", e.displayName());
    }

    @Test
    void displayName_noNameAtAll_returnsDefault() {
        LeagueEntry e = new LeagueEntry();
        assertEquals("Gracz", e.displayName());
    }

    // ---- winRatePct() -------------------------------------------------------

    @Test
    void winRatePct_withGames_calculatesCorrectly() {
        LeagueEntry e = new LeagueEntry();
        e.setWins(75);
        e.setLosses(25);
        assertEquals(75.0, e.winRatePct(), 0.001);
    }

    @Test
    void winRatePct_noGames_returnsZero() {
        LeagueEntry e = new LeagueEntry();
        e.setWins(0);
        e.setLosses(0);
        assertEquals(0.0, e.winRatePct(), 0.001);
    }

    @Test
    void totalGames_sumsWinsAndLosses() {
        LeagueEntry e = new LeagueEntry();
        e.setWins(30);
        e.setLosses(20);
        assertEquals(50, e.totalGames());
    }
}
