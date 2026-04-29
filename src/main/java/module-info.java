module com.yuhyfe.loldraftanalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;


    opens com.yuhyfe.loldraftanalyzer to javafx.fxml;
    exports com.yuhyfe.loldraftanalyzer;
}