package com.yuhyfe.loldraftanalyzer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChampSelectSession {
    private int localPlayerCellId;
    private List<ChampSelectPlayer> myTeam;
    private List<ChampSelectPlayer> theirTeam;
}
