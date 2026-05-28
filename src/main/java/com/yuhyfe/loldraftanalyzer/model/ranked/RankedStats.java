package com.yuhyfe.loldraftanalyzer.model.ranked;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class RankedStats {
    private Map<String, RankedEntry> queueMap;

    public RankedEntry getSolo() {
        if (queueMap == null) return new RankedEntry();
        return queueMap.getOrDefault("RANKED_SOLO_5x5", new RankedEntry());
    }

    public RankedEntry getFlex() {
        if (queueMap == null) return new RankedEntry();
        return queueMap.getOrDefault("RANKED_FLEX_SR", new RankedEntry());
    }
}
