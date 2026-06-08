package com.yuhyfe.loldraftanalyzer.model.champion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChampionEntry {

    private String id;
    private String name;
    private int championKey;

    private List<String> tags;

    private double winRate;
    private double pickRate;
    private double banRate;
    private String tier = "";

    public String primaryTag() {
        return (tags != null && !tags.isEmpty()) ? tags.get(0) : "";
    }

    public boolean matchesRole(String filterRole) {
        if ("ALL".equals(filterRole)) return true;
        if (tags == null || tags.isEmpty()) return false;
        for (String tag : tags) {
            switch (tag) {
                case "Marksman" -> { if ("ADC".equals(filterRole)) return true; }
                case "Support"  -> { if ("SUPPORT".equals(filterRole)) return true; }
                case "Mage"     -> { if ("MID".equals(filterRole) || "SUPPORT".equals(filterRole)) return true; }
                case "Assassin" -> { if ("MID".equals(filterRole) || "JUNGLE".equals(filterRole)) return true; }
                case "Fighter"  -> { if ("TOP".equals(filterRole) || "JUNGLE".equals(filterRole)) return true; }
                case "Tank"     -> { if ("TOP".equals(filterRole) || "SUPPORT".equals(filterRole)) return true; }
            }
        }
        return false;
    }

    public String roleCssClass() {
        return switch (primaryTag()) {
            case "Marksman" -> "adc";
            case "Support"  -> "sup";
            case "Mage", "Assassin" -> "mid";
            case "Fighter", "Tank"  -> "top";
            default -> "jungle";
        };
    }

    public String roleLabel() {
        return switch (primaryTag()) {
            case "Marksman" -> "ADC";
            case "Support"  -> "SUP";
            case "Mage"     -> "MID";
            case "Assassin" -> "MID";
            case "Fighter"  -> "TOP";
            case "Tank"     -> "TOP";
            default -> "JG";
        };
    }

    public boolean hasTier() {
        return tier != null && !tier.isBlank();
    }
}
