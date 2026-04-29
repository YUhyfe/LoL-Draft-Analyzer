package com.yuhyfe.loldraftanalyzer.lcu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LcuConnector {

    public String[] readLockFile() throws IOException {
        Path path = Path.of("C:/Riot Games/League of Legends/lockfile");

        String content = Files.readString(path);

        return content.split(":");
    }

}
