package com.yuhyfe.loldraftanalyzer.model.champselect;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChampSelectPlayer {
    private int cellId;
    private int championId;
    private String assignedPosition;
    private String gameName;
    private int team;
}
