package com.cbyte;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

public class PrimaryController implements Initializable {

    @FXML
    Button minBtn, maxBtn, closeBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FileChooser fileChooser = new FileChooser();

    }

    //button methods
    @FXML
    private void onClose() throws IOException {
       App.onClose();
    }

    @FXML
    private void onMax() throws IOException {
        App.onMax();
    }

    @FXML
    private void onMin() throws IOException {
        App.onMin();
    }
}
