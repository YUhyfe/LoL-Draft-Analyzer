package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Game {
    private long gameId;
    private long gameCreation;
    private long gameDuration;
    private int queueId;
    private List<ParticipantIdentity> participantIdentities;
    private List<Participant> participants;
    private List<TeamStats> teams;
}
