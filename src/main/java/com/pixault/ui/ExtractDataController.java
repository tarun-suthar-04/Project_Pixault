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
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for extracting hidden data from a stego image.
 */
public class ExtractDataController {

    private static final Logger log = LoggerFactory.getLogger(ExtractDataController.class);

    @FXML private Label selectedImageLabel;
    @FXML private PasswordField passwordField;
    @FXML private Button extractBtn;
    @FXML private Label statusLabel;
    @FXML private TextArea resultArea;
    @FXML private Pane loadingOverlay;

    private File selectedImage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        statusLabel.setText("");
        selectedImageLabel.setText("No image selected");
        resultArea.setEditable(false);
        resultArea.setPromptText("Extracted secret will appear here…");
        if (loadingOverlay != null) loadingOverlay.setVisible(false);
    }

    @FXML
    private void onSelectImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Stego Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = chooser.showOpenDialog(extractBtn.getScene().getWindow());
        if (file != null) {
            selectedImage = file;
            selectedImageLabel.setText(file.getName());
            showInfo("Image selected. Enter your password and extract.");
        }
    }

    @FXML
    private void onExtract() {
        if (selectedImage == null) {
            showError("Select a stego image first.");
            return;
        }
        char[] password = passwordField.getText().toCharArray();
        if (password.length < 4) {
            showError("Enter the password used during hiding.");
            return;
        }

        extractBtn.setDisable(true);
        resultArea.clear();
        if (loadingOverlay != null) loadingOverlay.setVisible(true);
        showInfo("⚙ Decrypting and extracting… please wait.");

        executor.submit(() -> {
            try {
                byte[] imageBytes = Files.readAllBytes(selectedImage.toPath());
                String payload = StegoEngine.extractAndDecrypt(imageBytes, password);
                java.util.Arrays.fill(password, '\0');

                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    resultArea.setText(payload);
                    showSuccess("✓ Data extracted and integrity verified.");
                    logAuditExtract();
                    extractBtn.setDisable(false);
                });
            } catch (StegoException e) {
                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    showError("✗ " + e.getMessage());
                    extractBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisible(false);
                    showError("Error: " + e.getMessage());
                    extractBtn.setDisable(false);
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

    @FXML
    private void onCopyResult() {
        String text = resultArea.getText();
        if (!text.isBlank()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            showSuccess("✓ Copied to clipboard.");
        }
    }

    private void logAuditExtract() {
        try {
            Session s = DashboardController.currentSession;
            if (s != null) {
                new com.pixault.database.ShareDAO().insertAuditLog(
                        new AuditLog(s.getUserId(), AuditLog.EXTRACT_DATA,
                                null, com.pixault.auth.DeviceFingerprint.generate(), "Data extracted from image"));
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
