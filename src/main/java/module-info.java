module com.yuhyfe.loldraftanalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;


    opens com.yuhyfe.loldraftanalyzer to javafx.fxml;
    opens com.yuhyfe.loldraftanalyzer.model to org.hibernate.orm.core;
    exports com.yuhyfe.loldraftanalyzer;
}