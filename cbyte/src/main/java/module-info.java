module com.cbyte {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.kodedu.terminalfx;
    requires atlantafx.base;

    opens com.cbyte to javafx.fxml;
    exports com.cbyte;
}
