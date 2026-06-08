package com.yuhyfe.loldraftanalyzer.model.riot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class RiotMatchInfo {
    private String gameVersion;
    private int    queueId;
    private List<RiotParticipant> participants;
    private List<RiotTeam>        teams;
}
