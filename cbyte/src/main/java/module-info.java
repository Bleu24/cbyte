module com.cbyte {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.cbyte to javafx.fxml;
    exports com.cbyte;
}
