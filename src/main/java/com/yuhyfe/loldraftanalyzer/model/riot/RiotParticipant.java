package com.yuhyfe.loldraftanalyzer.model.riot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class RiotParticipant {
    /** DDragon champion name key, e.g. "Aatrox", "LeeSin". */
    private String championName;
    private int    teamId;         // 100 or 200
    private boolean win;
    /** "TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY" */
    private String teamPosition;
    private int kills;
    private int deaths;
    private int assists;
}
