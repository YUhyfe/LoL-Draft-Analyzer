package com.yuhyfe.loldraftanalyzer.util;

import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public final class AppLogger {

    private static final String ROOT = "com.yuhyfe.loldraftanalyzer";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    static {
        Logger root = Logger.getLogger(ROOT);
        if (root.getHandlers().length == 0) {
            root.addHandler(buildHandler());
            root.setUseParentHandlers(false);
        }
        root.setLevel(Level.ALL);
    }

    private AppLogger() {}

    public static Logger get(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    private static Handler buildHandler() {
        Handler handler = new StreamHandler(System.out, new Formatter() {
            @Override
            public String format(LogRecord r) {
                String time  = TIME_FMT.format(Instant.ofEpochMilli(r.getMillis()));
                String level = levelLabel(r.getLevel());
                String name  = shortName(r.getLoggerName());
                String msg   = formatMessage(r);

                StringBuilder sb = new StringBuilder();
                sb.append(time).append(" ").append(level).append(" [").append(name).append("] ").append(msg).append("\n");

                if (r.getThrown() != null) {
                    sb.append("  caused by: ").append(r.getThrown()).append("\n");
                }
                return sb.toString();
            }
        }) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        handler.setLevel(Level.ALL);
        return handler;
    }

    private static String levelLabel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue())  return "ERROR";
        if (level.intValue() >= Level.WARNING.intValue()) return "WARN ";
        if (level.intValue() >= Level.INFO.intValue())    return "INFO ";
        return "DEBUG";
    }

    private static String shortName(String loggerName) {
        if (loggerName == null) return "?";
        int dot = loggerName.lastIndexOf('.');
        return dot >= 0 ? loggerName.substring(dot + 1) : loggerName;
    }
}
