package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Participant {
    private int participantId;
    private int teamId;       // 100 or 200
    private int championId;
    private ParticipantStats stats;
    private ParticipantTimeline timeline;
}
