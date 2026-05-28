package com.yuhyfe.loldraftanalyzer.model.summoner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Summoner {
    private long accountId;
    private String displayName;
    private String gameName;
    private String internalName;
    private int profileIconId;
    private String puuid;
    private long summonerId;
    private long summonerLevel;
    private String tagLine;
    private String platformId;
}
