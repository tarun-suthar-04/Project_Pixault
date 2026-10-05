package com.pixault;

import com.pixault.share.ShareHttpServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Pixault – Secure Steganography & Encrypted Digital Vault
 * Main JavaFX Application entry point.
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static Stage primaryStage;

    public static boolean isDarkMode = false;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        com.pixault.database.DatabaseInitializer.initialize();
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Scene scene = new Scene(root, 1100, 700);
            applyTheme(scene);
            stage.setTitle("🔐 Pixault – Secure Vault");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.show();

            ShareHttpServer.startIfConfigured();
            log.info("Pixault started.");
        } catch (Exception e) {
            log.error("Failed to start Pixault: {}", e.getMessage(), e);
        }
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("/css/styles.css")).toExternalForm());
        if (isDarkMode) {
            scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("/css/dark-mode.css")).toExternalForm());
        }
    }

    @Override
    public void stop() {
        ShareHttpServer.stop();
    }

    /**
     * Helper to switch the primary scene from any controller.
     */
    public static void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(Main.class.getResource(fxmlPath)));
            Scene scene = new Scene(root, primaryStage.getScene().getWidth(),
                    primaryStage.getScene().getHeight());
            applyTheme(scene);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            log.error("Scene switch failed for {}: {}", fxmlPath, e.getMessage(), e);
        }
    }

    /**
     * Helper to switch while passing a controller factory (for dependency
     * injection).
     */
    public static void switchScene(String fxmlPath, javafx.util.Callback<Class<?>, Object> factory) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(Main.class.getResource(fxmlPath)));
            loader.setControllerFactory(factory);
            Parent root = loader.load();
            Scene scene = new Scene(root, primaryStage.getScene().getWidth(),
                    primaryStage.getScene().getHeight());
            applyTheme(scene);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            log.error("Scene switch failed: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
