module com.yuhyfe.loldraftanalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.net.http;
    requires static lombok;
    requires com.google.gson;

    opens com.yuhyfe.loldraftanalyzer to javafx.fxml;
    opens com.yuhyfe.loldraftanalyzer.model.summoner to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.ranked to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.match to com.google.gson;
    opens com.yuhyfe.loldraftanalyzer.model.champselect to com.google.gson;
    exports com.yuhyfe.loldraftanalyzer;
}