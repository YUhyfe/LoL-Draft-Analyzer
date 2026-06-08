package com.yuhyfe.loldraftanalyzer.model.ranked;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RankedStats {
    private List<RankedEntry> queues;

    public RankedEntry getSolo() {
        return find("RANKED_SOLO_5x5");
    }

    public RankedEntry getFlex() {
        return find("RANKED_FLEX_SR");
    }

    private RankedEntry find(String queueType) {
        if (queues == null) return new RankedEntry();
        return queues.stream()
                .filter(e -> queueType.equals(e.getQueueType()))
                .findFirst()
                .orElse(new RankedEntry());
    }
}
