module Finance.Finance {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Database
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Logging
    requires java.logging;

    // SLF4J — needed by sqlite-jdbc at runtime
    requires org.slf4j;

    /*
     * Opens Finance.Finance to javafx.fxml so FXMLLoader can
     * reflectively instantiate and @FXML-inject ALL controllers:
     * LoginController, MainController, OverviewController,
     * ExportController, AnomalyController
     */
    opens Finance.Finance to javafx.fxml;

    // Expose App to the JVM launcher
    exports Finance.Finance;
}