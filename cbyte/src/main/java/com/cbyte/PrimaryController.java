package com.cbyte;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.event.ActionEvent;

// TODO: terminal, logic

public class PrimaryController implements Initializable {

    @FXML
    private Button minBtn, maxBtn, closeBtn, openBtn;

    @FXML
    private ScrollPane scrollPane;
    
    @FXML
    private TilePane tilePane;
    
    private static final double ICON_SIZE = 48.0; // Larger icon size
    private static final double TILE_SIZE = 100.0; // Size of each tile

    private Image folderIcon = new Image(getClass().getResourceAsStream("/images/folder.png"));
    private Image fileIcon = new Image(getClass().getResourceAsStream("/images/cpp.png"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tilePane = new TilePane();
        tilePane.setPrefColumns(5); // Adjust the number of columns as needed
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        scrollPane.setContent(tilePane);
    }

    @FXML
    private void handleOpenButton(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            displayFileTree(selectedDirectory);
        }
    }

    private void displayFileTree(File directory) {
        tilePane.getChildren().clear();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                addFileToTilePane(file);
            }
        }
    }

    private void addFileToTilePane(File file) {
        VBox fileBox = new VBox(5);
        fileBox.setPrefSize(TILE_SIZE, TILE_SIZE);
        fileBox.setAlignment(javafx.geometry.Pos.CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(ICON_SIZE);
        imageView.setFitHeight(ICON_SIZE);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(file.getName());
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        if (file.isDirectory()) {
            imageView.setImage(folderIcon);
        } else {
            imageView.setImage(fileIcon);
        }

        fileBox.getChildren().addAll(imageView, nameLabel);
        
        // Add hover effect
        fileBox.setOnMouseEntered(e -> fileBox.setStyle("-fx-background-color: lightblue;"));
        fileBox.setOnMouseExited(e -> fileBox.setStyle(""));

        Platform.runLater(() -> tilePane.getChildren().add(fileBox));
    }

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