package com.cbyte;

import atlantafx.base.theme.Dracula;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage thisStage;
    
    
    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"));

        // Apply the Dracula theme
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());

        // Load the custom font
        Font font = Font.loadFont(App.class.getResource("/fonts/FiraCode-VariableFont_wght.ttf").toExternalForm(), 14);

        thisStage = stage;
        thisStage.setTitle("TerminalFX");
        thisStage.initStyle(StageStyle.UNDECORATED);
        thisStage.setScene(scene);
        thisStage.show();
    }
    

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

    //button methods
    public static void onMax() {
        thisStage.setMaximized(!thisStage.isMaximized());
    }

    public static void onMin() {
        thisStage.setIconified(true);
    }

    public static void onClose() {
        Platform.exit();
        System.exit(0);
    }
}