package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ParticipantIdentity {
    private int participantId;
    private MatchPlayer player;
}
