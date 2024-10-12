package com.cbyte;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
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


public class PrimaryController implements Initializable {

    @FXML
    private Button minBtn, maxBtn, closeBtn, openBtn;

    @FXML
    private TreeView<File> treeView;
    private static final double ICON_SIZE = 16.0; // or whatever size you prefer

    private Image folderIcon = new Image(getClass().getResourceAsStream("/images/folder.png"));
    private Image fileIcon = new Image(getClass().getResourceAsStream("/images/cpp.png"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
       treeView.setCellFactory(param -> new TreeCell<File>() {
            private ImageView imageView = new ImageView();
            private Label nameLabel = new Label();
            private Label detailsLabel = new Label();
            private HBox hbox = new HBox(5);

            {
                imageView.setFitWidth(ICON_SIZE);
                imageView.setFitHeight(ICON_SIZE);
                imageView.setPreserveRatio(true);
                hbox.getChildren().addAll(imageView, nameLabel, detailsLabel);
            }

            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    nameLabel.setText(file.getName());
                    if (file.isDirectory()) {
                        imageView.setImage(folderIcon);
                        detailsLabel.setText("Directory");
                    } else {
                        imageView.setImage(fileIcon);
                        detailsLabel.setText(String.format("%.2f KB", file.length() / 1024.0)); // displaying file size
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    detailsLabel.setText(detailsLabel.getText() + " - " + sdf.format(file.lastModified()));
                    setGraphic(hbox);
                }
                
            }
       });

       treeView.setRoot(null);
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
        TreeItem<File> root = createNode(directory);
        treeView.setRoot(root);
    }

    private TreeItem<File> createNode(final File f) {
        return new TreeItem<File>(f) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    File f = getValue();
                    isLeaf = f.isFile();
                }
                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
            File f = TreeItem.getValue();
            if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
                if (files != null) {
                    ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                    for (File childFile : files) {
                        children.add(createNode(childFile));
                    }
                        return children;
                    }
                }
                return FXCollections.emptyObservableList();
        }
    };
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