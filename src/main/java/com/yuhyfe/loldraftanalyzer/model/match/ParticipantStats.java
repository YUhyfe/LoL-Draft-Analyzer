package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ParticipantStats {
    private int kills;
    private int deaths;
    private int assists;
    private boolean win;
}
