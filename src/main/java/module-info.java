module com.yuhyfe.loldraftanalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.net.http;
    requires static lombok;
    requires com.google.gson;

    opens com.yuhyfe.loldraftanalyzer to javafx.fxml, com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.controller to javafx.fxml;
    opens com.yuhyfe.loldraftanalyzer.model.summoner to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.ranked to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.match to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.champselect to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.champion to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.meta to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.riot to com.google.gson;

    exports com.yuhyfe.loldraftanalyzer;
    exports com.yuhyfe.loldraftanalyzer.controller;
    exports com.yuhyfe.loldraftanalyzer.util;
    exports com.yuhyfe.loldraftanalyzer.model.match;
    exports com.yuhyfe.loldraftanalyzer.model.ranked;
}
