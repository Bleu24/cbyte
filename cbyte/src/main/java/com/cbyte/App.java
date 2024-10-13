package com.cbyte;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        scene.getStylesheets().add(App.class.getResource("/styles.css").toExternalForm());
        
        thisStage = stage;
        thisStage.setTitle("TerminalFX");
        thisStage.initStyle(StageStyle.UNDECORATED);
        thisStage.setScene(scene);
        thisStage.show();
    }

        /**
             * The main() method is ignored in correctly deployed JavaFX application.
             * main() serves only as fallback in case the application can not be
             * launched through deployment artifacts, e.g., in IDEs with limited FX
             * support. NetBeans ignores main().
             *
             * @param args the command line arguments
             */
    

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