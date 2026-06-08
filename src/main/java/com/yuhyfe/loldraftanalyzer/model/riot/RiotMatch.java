package com.yuhyfe.loldraftanalyzer.model.riot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class RiotMatch {
    // metadata is a JSON object in the API response — we only need 'info'
    private RiotMatchInfo info;
}
