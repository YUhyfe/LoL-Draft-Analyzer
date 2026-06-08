package com.yuhyfe.loldraftanalyzer.model.match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class TeamStats {
    private int teamId;
    private List<Ban> bans;
    private String win; // "Win" | "Fail"
}
