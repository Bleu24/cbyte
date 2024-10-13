module com.cbyte {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.kodedu.terminalfx;
    
    opens com.cbyte to javafx.fxml;
    exports com.cbyte;
}
