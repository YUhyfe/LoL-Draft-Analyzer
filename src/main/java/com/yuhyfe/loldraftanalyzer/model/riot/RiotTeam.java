package com.yuhyfe.loldraftanalyzer.model.riot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class RiotTeam {
    private int teamId;
    private boolean win;
    private List<RiotBan> bans;
}
