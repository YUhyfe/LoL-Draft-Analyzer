package com.yuhyfe.loldraftanalyzer.model.match;

import com.yuhyfe.loldraftanalyzer.util.MatchUtils;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatchSummary {
    private int    championId;
    private String championName;
    private int    kills;
    private int    deaths;
    private int    assists;
    private boolean win;
    private long   durationSeconds;
    private long   gameCreationMs;
    private int    queueId;
    private String lane;

    public String getFormattedDuration() {
        return MatchUtils.formatDuration(durationSeconds);
    }

    public String getFormattedTimeAgo() {
        return MatchUtils.timeAgo(gameCreationMs);
    }

    public String getQueueName() {
        return MatchUtils.queueName(queueId);
    }

    public String getKdaString() {
        return kills + " / " + deaths + " / " + assists;
    }

    public String getFormattedKda() {
        double ratio = deaths == 0 ? (kills + assists) : (kills + assists) / (double) deaths;
        return String.format("KDA %.2f", ratio);
    }

    public String getLaneDisplay() {
        return lane == null || lane.isBlank() ? "—" : MatchUtils.laneLabel(lane);
    }
}
