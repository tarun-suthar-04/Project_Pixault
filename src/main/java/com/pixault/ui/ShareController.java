package com.pixault.ui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.pixault.Main;
import com.pixault.model.Session;
import com.pixault.model.VaultShare;
import com.pixault.share.ShareService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Share Vault controller for creating one-time secure sharing links.
 */
public class ShareController {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    @FXML
    private Label selectedBlobLabel;
    @FXML
    private TextField ttlField;
    @FXML
    private PasswordField sharePasswordField;
    @FXML
    private Button createShareBtn;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField generatedLinkField;
    @FXML
    private Button copyLinkBtn;
    @FXML
    private Button openLinkBtn;
    @FXML
    private TableView<VaultShare> sharesTable;
    @FXML
    private TableColumn<VaultShare, String> colId;
    @FXML
    private TableColumn<VaultShare, String> colExpiry;
    @FXML
    private TableColumn<VaultShare, Boolean> colConsumed;

    private Path selectedBlobPath;
    private final ShareService shareService = new ShareService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        generatedLinkField.setEditable(false);
        ttlField.setText("60");
        statusLabel.setText("");
        copyLinkBtn.setDisable(true);
        if (openLinkBtn != null) {
            openLinkBtn.setDisable(true);
        }

        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getShareId().substring(0, 8) + "..."));
        colExpiry.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getExpiryTime().toString()));
        colConsumed.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(
                d.getValue().isConsumed()));

        loadShares();
    }

    @FXML
    private void onSelectBlob() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Stego Image to Share");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PNG Image", "*.png"));

        File file = chooser.showOpenDialog(createShareBtn.getScene().getWindow());
        if (file != null) {
            selectedBlobPath = file.toPath();
            selectedBlobLabel.setText(file.getName());
        }
    }

    @FXML
    private void onCreateShare() {
        if (selectedBlobPath == null) {
            showError("Select a stego image first.");
            return;
        }

        int ttl;
        try {
            ttl = Integer.parseInt(ttlField.getText().trim());
            if (ttl <= 0) {
                showError("TTL must be greater than 0 minutes.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("TTL must be a number (minutes).");
            return;
        }

        Session s = DashboardController.currentSession;
        if (s == null) {
            showError("No active session.");
            return;
        }

        createShareBtn.setDisable(true);
        showInfo("Creating secure share link...");
        char[] sharePassword = sharePasswordField.getText().toCharArray();
        if (sharePassword.length < 4) {
            showError("Enter stego decryption password to include one-time message.");
            createShareBtn.setDisable(false);
            return;
        }

        final int finalTtl = ttl;
        final char[] finalPassword = Arrays.copyOf(sharePassword, sharePassword.length);
        Arrays.fill(sharePassword, '\0');
        executor.submit(() -> {
            try {
                String link = shareService.createShare(s.getUserId(), selectedBlobPath, finalTtl, finalPassword);
                Arrays.fill(finalPassword, '\0');
                Platform.runLater(() -> {
                    generatedLinkField.setText(link);
                    copyLinkBtn.setDisable(false);
                    if (openLinkBtn != null) {
                        openLinkBtn.setDisable(false);
                    }
                    showSuccess("One-time share link created (expires in " + finalTtl + " min).");
                    createShareBtn.setDisable(false);
                    loadShares();
                });
            } catch (Exception e) {
                Arrays.fill(finalPassword, '\0');
                Platform.runLater(() -> {
                    showError("Failed: " + e.getMessage());
                    createShareBtn.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onCopyLink() {
        String link = generatedLinkField.getText();
        if (link != null && !link.isBlank()) {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(link);
            Clipboard.getSystemClipboard().setContent(cc);
            showSuccess("Link copied to clipboard.");
        }
    }

    @FXML
    private void onOpenLink() {
        String link = generatedLinkField.getText();
        if (link == null || link.isBlank()) {
            showError("Generate a share link first.");
            return;
        }

        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showError("Browser open is not supported on this system.");
                return;
            }
            Desktop.getDesktop().browse(URI.create(link));
            showSuccess("Share link opened in browser.");
        } catch (Exception e) {
            showError("Failed to open browser: " + e.getMessage());
        }
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

    private void loadShares() {
        Session s = DashboardController.currentSession;
        if (s == null) {
            return;
        }
        executor.submit(() -> {
            try {
                List<VaultShare> shares = shareService.getSharesForUser(s.getUserId());
                Platform.runLater(() -> sharesTable.setItems(FXCollections.observableArrayList(shares)));
            } catch (Exception ignored) {
            }
        });
    }

    private void showError(String m) {
        statusLabel.setText(m);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void showSuccess(String m) {
        statusLabel.setText(m);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void showInfo(String m) {
        statusLabel.setText(m);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }
}
