package com.yuhyfe.loldraftanalyzer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yuhyfe.loldraftanalyzer.util.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class AppSettings {

    private static final Logger LOG = AppLogger.get(AppSettings.class);

    private static final Path DIR  = Path.of(System.getenv("APPDATA"), "LoLDraftAnalyzer");
    private static final Path FILE = DIR.resolve("settings.json");

    private static volatile AppSettings instance;

    private String riotApiKey = "";
    private String region     = "";
    private String lolPath    = "";

    private AppSettings() {}

    public static AppSettings get() {
        if (instance == null) {
            synchronized (AppSettings.class) {
                if (instance == null) {
                    instance = load();
                }
            }
        }
        return instance;
    }

    private static AppSettings load() {
        AppSettings s = new AppSettings();
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("riotApiKey") && !obj.get("riotApiKey").isJsonNull())
                    s.riotApiKey = obj.get("riotApiKey").getAsString();
                if (obj.has("region") && !obj.get("region").isJsonNull())
                    s.region = obj.get("region").getAsString();
                if (obj.has("lolPath") && !obj.get("lolPath").isJsonNull())
                    s.lolPath = obj.get("lolPath").getAsString();
            }
        } catch (Exception e) {
            LOG.warning("load error: " + e.getMessage());
        }
        LOG.info("loaded — keyLen=" + s.riotApiKey.length() + "  region=[" + s.region + "]");
        return s;
    }

    public void save() {
        try {
            Files.createDirectories(DIR);
            JsonObject obj = new JsonObject();
            obj.addProperty("riotApiKey", riotApiKey);
            obj.addProperty("region", region);
            obj.addProperty("lolPath", lolPath);
            Files.writeString(FILE, obj.toString());
            LOG.info("saved — keyLen=" + riotApiKey.length() + "  region=[" + region + "]");
        } catch (IOException e) {
            LOG.severe("save error: " + e.getMessage());
        }
    }

    public String  getRiotApiKey()         { return riotApiKey == null ? "" : riotApiKey; }
    public void    setRiotApiKey(String v) { this.riotApiKey = v == null ? "" : v.strip(); }

    public boolean hasApiKey()             { return !getRiotApiKey().isBlank(); }

    public String  getRegion()             { return region == null ? "" : region; }
    public void    setRegion(String v)     { this.region = v == null ? "" : v; }

    public String  getLolPath()            { return lolPath == null ? "" : lolPath; }
    public void    setLolPath(String v)    { this.lolPath = v == null ? "" : v.strip(); }

    public Path getLockfilePath() {
        String p = getLolPath();
        if (!p.isBlank()) return Path.of(p, "lockfile");
        for (String candidate : new String[]{
                "C:/Riot Games/League of Legends",
                "D:/Riot Games/League of Legends",
                System.getenv("LOCALAPPDATA") + "/Riot Games/League of Legends"}) {
            Path candidatePath = Path.of(candidate, "lockfile");
            if (Files.exists(candidatePath)) return candidatePath;
        }
        return Path.of("C:/Riot Games/League of Legends/lockfile");
    }

    public String getRoutingHost() {
        return switch (getRegion().toUpperCase()) {
            case "EUW", "EUNE", "TR", "RU" -> "europe";
            case "NA", "BR", "LAN", "LAS"  -> "americas";
            case "KR", "JP"                -> "asia";
            default                        -> "europe";
        };
    }

    public String getPlatformHost() {
        return switch (getRegion().toUpperCase()) {
            case "EUW"  -> "euw1";
            case "EUNE" -> "eun1";
            case "NA"   -> "na1";
            case "KR"   -> "kr";
            case "JP"   -> "jp1";
            case "BR"   -> "br1";
            case "LAN"  -> "la1";
            case "LAS"  -> "la2";
            case "TR"   -> "tr1";
            case "RU"   -> "ru";
            case "OCE"  -> "oc1";
            default     -> "euw1";
        };
    }
}
