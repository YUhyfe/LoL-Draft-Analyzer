package com.yuhyfe.loldraftanalyzer.util;

import com.yuhyfe.loldraftanalyzer.model.match.Game;
import com.yuhyfe.loldraftanalyzer.model.match.Participant;
import com.yuhyfe.loldraftanalyzer.model.match.ParticipantIdentity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class MatchUtils {

    private MatchUtils() {}

    public static Participant findParticipant(Game game, String gameName) {
        if (game.getParticipants() == null || game.getParticipants().isEmpty()) return null;
        if (gameName != null && !gameName.isBlank() && game.getParticipantIdentities() != null) {
            for (ParticipantIdentity pi : game.getParticipantIdentities()) {
                if (pi.getPlayer() != null
                        && gameName.equalsIgnoreCase(pi.getPlayer().getGameName())) {
                    int pid = pi.getParticipantId();
                    return game.getParticipants().stream()
                            .filter(p -> p.getParticipantId() == pid)
                            .findFirst().orElse(null);
                }
            }
        }
        return game.getParticipants().get(0);
    }

    public static String normalizeLane(String lane, String role) {
        if (lane == null) return "NONE";
        if ("BOTTOM".equals(lane)
                && ("DUO_SUPPORT".equals(role) || "UTILITY".equals(role))) {
            return "UTILITY";
        }
        return lane;
    }

    public static String laneLabel(String lane) {
        if (lane == null) return "";
        return switch (lane.toUpperCase()) {
            case "TOP"            -> "Top";
            case "JUNGLE"         -> "Jungle";
            case "MIDDLE", "MID"  -> "Mid";
            case "BOTTOM"         -> "Bot";
            case "UTILITY"        -> "Support";
            default               -> "";
        };
    }

    public static String queueName(int queueId) {
        return switch (queueId) {
            case 420       -> "Solo/Duo";
            case 440       -> "Flex";
            case 400       -> "Normal Draft";
            case 430       -> "Normal Blind";
            case 450       -> "ARAM";
            case 830, 840, 850 -> "vs AI";
            case 0         -> "Niestandardowa";
            default        -> "Tryb #" + queueId;
        };
    }

    public static String formatDuration(long totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    public static String timeAgo(long epochMs) {
        if (epochMs <= 0) return "";
        long minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(epochMs), Instant.now());
        if (minutes < 60)         return minutes + " min temu";
        long hours = minutes / 60;
        if (hours < 24)           return hours + " godz. temu";
        long days = hours / 24;
        if (days == 1)            return "wczoraj";
        if (days < 30)            return days + " dni temu";
        return (days / 30) + " mies. temu";
    }
}
