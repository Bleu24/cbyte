package com.cbyte;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.application.Platform;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import com.kodedu.terminalfx.config.TerminalConfig;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import java.util.ResourceBundle;

import com.cbyte.classes.Interpreter;

import static com.cbyte.classes.Interpreter.*;

public class PrimaryController implements Initializable {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TilePane tilePane;
    @FXML
    private TabPane tabPane;
    @FXML
    private TextArea textArea; // Reference to the TextArea

    private static TerminalTab terminalTab;
    private Font font;
    private File currentFile;
    private static final double ICON_SIZE = 48.0;
    private static final double TILE_SIZE = 100.0;
    private final Image folderIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/folder-3d.png")));
    private final Image fileIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cpp-3d.png")));
    private static String fileName;
    private static final String OUTPUT_FILE_PATH = "output.txt";
    private static String sourceCode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        System.out.println("Initializing PrimaryController...");
        InputStream fontStream = getClass().getResourceAsStream("/fonts/FiraCode-VariableFont_wght.ttf");
        if (fontStream == null) {
            System.out.println("Font file not found!");
        } else {
            font = Font.loadFont(fontStream, 14);
            System.out.println("Font loaded!");
            System.out.println("Loaded font family: " + font.getFamily());
        }

        TerminalConfig darkConfig = new TerminalConfig();
        darkConfig.setBackgroundColor(Color.rgb(40, 42, 54));
        darkConfig.setForegroundColor(Color.rgb(248, 248, 242));
        darkConfig.setCursorColor(Color.rgb(255, 255, 255, 0.5));
        darkConfig.setFontFamily("Fira Code Light");

        TerminalBuilder terminalBuilder = new TerminalBuilder(darkConfig);
        terminalTab = terminalBuilder.newTerminal();
        tabPane.getTabs().add(terminalTab);

        tilePane.setPrefColumns(5);
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        scrollPane.setContent(tilePane);

        System.out.println("PrimaryController initialized successfully.");
    }

    // Function that allows the output to display in the embedded terminal (like 'yung system.out.println nadidisplay rin dun)
    public static void printToTerminal() {
        if (terminalTab != null) {
            terminalTab.getTerminal().onTerminalFxReady(() -> {
                Platform.runLater(() -> {
                    terminalTab.getTerminal().command("type " + OUTPUT_FILE_PATH + "\r");
                });
            });
        }
    }

    private void displayFileTree(File directory) {
        System.out.println("Displaying file tree for directory: " + directory.getAbsolutePath());
        tilePane.getChildren().clear();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                addFileToTilePane(file);
            }
            System.out.println("File tree displayed successfully.");
        } else {
            System.out.println("No files found in the directory.");
        }
    }

    private void addFileToTilePane(File file) {
        System.out.println("Adding file to TilePane: " + file.getName());
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
        fileBox.setOnMouseEntered(e -> fileBox.setStyle("-fx-background-color: #44475A;"));
        fileBox.setOnMouseExited(e -> fileBox.setStyle(""));

        // Handle file click to display content
        fileBox.setOnMouseClicked(e -> {
            if (file.isFile()) {
                displayFileContent(file);
                currentFile = file; // Sets the file displayed as the file which is to be interpreted

                // Get the filename without the extension
                fileName = file.getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    fileName = fileName.substring(0, dotIndex);
                }

                System.out.println("Filename without extension: " + fileName);
            }
        });


        Platform.runLater(() -> {
            tilePane.getChildren().add(fileBox);
            System.out.println("File added to TilePane: " + file.getName());
        });
    }

    private void displayFileContent(File file) {
        System.out.println("Displaying content of file: " + file.getAbsolutePath());
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            System.out.println("File content displayed successfully.");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            textArea.setText("Error reading file: " + e.getMessage());
        }
    }

    // Function to execute the created .exe file using terminalTab
    private static void compileAndRunCode() {

        if (terminalTab != null) {
            String comError = "Error during execution: ";

            bufferMessage("Executing...");
            writeMessagesToFile();
            printToTerminal();

            terminalTab.getTerminal().onTerminalFxReady(() -> {
                Platform.runLater(() -> {
                    try {
                        // Command to execute the .exe file
                        String command = fileName + "\r";
                        terminalTab.getTerminal().command(command);            // Execute the file
                    } catch (Exception e) {
                        String errorMessage = comError + e.getMessage();
                        bufferMessage(errorMessage);
                        writeMessagesToFile();
                        printToTerminal();
                        e.printStackTrace();
                    }
                });
            });
        } else {
            System.out.println("Error: terminalTab is null, cannot proceed with execution.");
        }
    }


    // Getter for fileName
    public static String getFileName() {
        return fileName;
    }

    // Getter and setter for sourceCode
    public static String getSourceCode() {
        return sourceCode;
    }

    public static void setSourceCode(String sourceCode) {
        PrimaryController.sourceCode = sourceCode;
    }

    //
//    FXML METHODS
//

    @FXML
    private void handleOpenButton(ActionEvent event) {
        System.out.println("Open Button clicked.");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            System.out.println("Directory selected: " + selectedDirectory.getAbsolutePath());
            displayFileTree(selectedDirectory);
        } else {
            System.out.println("No directory selected.");
        }
    }

    @FXML
    private void compileFile(ActionEvent event) {
        if (currentFile != null) {
            System.out.println("Compiling file: " + currentFile.getAbsolutePath());

            // Get the output from interpretFile
            Boolean output = Interpreter.interpretFile(currentFile);

            // Check if the output indicates an error
            if (!output) {
                sourceCode = "";
                System.out.println("Compilation halted due to an error.");
                return; // Stop further execution
            }

            String assemblyCode = translateToAssembly(sourceCode);

            if (assemblyCode != null) {
                System.out.println("Assembly code translation successful.");
                compileAndRunCode();
            } else {
                System.out.println("Assembly translation failed or contains errors.");
            }

        } else {
            System.out.println("No file selected to compile.");
        }
    }

    @FXML
    private void onClose() {
        System.out.println("Close button clicked.");
        App.onClose();
    }

    @FXML
    private void onMax() {
        System.out.println("Maximize button clicked.");
        App.onMax();
    }

    @FXML
    private void onMin() {
        System.out.println("Minimize button clicked.");
        App.onMin();
    }

}
