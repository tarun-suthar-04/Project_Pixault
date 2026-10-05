package com.pixault.ui;

import com.pixault.Main;
import com.pixault.model.AuditLog;
import com.pixault.model.Session;
import com.pixault.stego.StegoEngine;
import com.pixault.stego.StegoEngine.StegoException;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for the Hide Data screen.
 * Encrypts and embeds secret text into a cover image using StegoEngine.
 */
public class HideDataController {

    private static final Logger log = LoggerFactory.getLogger(HideDataController.class);

    @FXML private Label selectedImageLabel;
    @FXML private TextArea payloadArea;
    @FXML private PasswordField passwordField;
    @FXML private Button hideBtn;
    @FXML private Label statusLabel;
    @FXML private Label charCountLabel;
    @FXML private Pane loadingOverlay;

    private File selectedImage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        statusLabel.setText("");
        selectedImageLabel.setText("No image selected");
        payloadArea.textProperty().addListener((obs, old, val) -> charCountLabel.setText(val.length() + " characters"));
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
    }

    @FXML
    private void onSelectImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Cover Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File file = chooser.showOpenDialog(hideBtn.getScene().getWindow());
        if (file != null) {
            selectedImage = file;
            selectedImageLabel.setText(file.getName() + "  (" + file.length() / 1024 + " KB)");
            showInfo("Image selected. Enter your secret and password.");
        }
    }

    @FXML
    private void onHide() {
        if (selectedImage == null) {
            showError("Please select a cover image first.");
            return;
        }
        String payload = payloadArea.getText();
        char[] password = passwordField.getText().toCharArray();
        if (payload.isBlank()) {
            showError("Payload cannot be empty.");
            return;
        }
        if (password.length < 4) {
            showError("Password must be at least 4 characters.");
            return;
        }

        hideBtn.setDisable(true);
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        showInfo("⚙ Encrypting and embedding… please wait.");

        executor.submit(() -> {
            try {
                byte[] imageBytes = Files.readAllBytes(selectedImage.toPath());
                byte[] stegoBytes = StegoEngine.encryptAndHide(imageBytes, payload, password);
                java.util.Arrays.fill(password, '\0');

                // Save output
                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Save Stego Image As");
                    chooser.setInitialFileName("vault_" + System.currentTimeMillis() + ".png");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
                    
                    File outFile = chooser.showSaveDialog(hideBtn.getScene().getWindow());
                    if (outFile != null) {
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.write(stegoBytes);
                        } catch (Exception e) {
                            showError("Failed to save: " + e.getMessage());
                            return;
                        }
                        showSuccess("✓ Data hidden successfully in: " + outFile.getName());
                        logAuditHide();
                    }
                    hideBtn.setDisable(false);
                });
            } catch (StegoException e) {
                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    showError("✗ " + e.getMessage());
                    hideBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    showError("Error: " + e.getMessage());
                    hideBtn.setDisable(false);
                });
            }
        });
    }

    @FXML private void onDashboard() { com.pixault.Main.switchScene("/fxml/Dashboard.fxml"); }
    @FXML private void onHideData() { com.pixault.Main.switchScene("/fxml/HideData.fxml"); }
    @FXML private void onExtractData() { com.pixault.Main.switchScene("/fxml/ExtractData.fxml"); }
    @FXML private void onShareVault() { com.pixault.Main.switchScene("/fxml/Share.fxml"); }
    @FXML private void onAuditLog() { com.pixault.Main.switchScene("/fxml/AuditLog.fxml"); }
    @FXML private void onSettings() { com.pixault.Main.switchScene("/fxml/Settings.fxml"); }
    
    @FXML private void onLogout() {
        if (com.pixault.ui.DashboardController.currentSession != null) {
            com.pixault.auth.SessionManager.invalidateSession(com.pixault.ui.DashboardController.currentSession.getSessionId());
            com.pixault.ui.DashboardController.currentSession = null;
        }
        com.pixault.Main.switchScene("/fxml/Login.fxml");
    }

    private void logAuditHide() {
        try {
            Session s = DashboardController.currentSession;
            if (s != null) {
                new com.pixault.database.ShareDAO().insertAuditLog(
                        new com.pixault.model.AuditLog(s.getUserId(), AuditLog.HIDE_DATA,
                                null, com.pixault.auth.DeviceFingerprint.generate(), "Data hidden in image"));
            }
        } catch (Exception ignored) {}
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.RED);
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
    }
}
