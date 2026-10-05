package com.pixault.ui;

import com.pixault.Main;
import com.pixault.model.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main vault dashboard controller.
 * Provides navigation to all vault features.
 */
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    /** Set by LoginController after successful authentication. */
    public static Session currentSession;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label sessionInfoLabel;
    @FXML
    private Label deviceLabel;

    @FXML
    public void initialize() {
        if (currentSession != null) {
            welcomeLabel.setText("Welcome, " + currentSession.getUsername() + " 👋");
            sessionInfoLabel.setText("Session expires: " + currentSession.getExpiresAt()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm, dd MMM yyyy")));
            String fp = currentSession.getDeviceFingerprint();
            deviceLabel.setText("Device: " + fp.substring(0, 8).toUpperCase() + "…");
        } else {
            welcomeLabel.setText("Welcome to Pixault");
        }
    }

    @FXML
    private void onHideData() {
        Main.switchScene("/fxml/HideData.fxml");
    }

    @FXML
    private void onExtractData() {
        Main.switchScene("/fxml/ExtractData.fxml");
    }

    @FXML
    private void onShareVault() {
        Main.switchScene("/fxml/Share.fxml");
    }

    @FXML
    private void onSettings() {
        Main.switchScene("/fxml/Settings.fxml");
    }

    @FXML
    private void onAuditLog() {
        Main.switchScene("/fxml/AuditLog.fxml");
    }

    @FXML
    private void onLogout() {
        if (currentSession != null) {
            com.pixault.auth.SessionManager.invalidateSession(currentSession.getSessionId());
            log.info("Session invalidated on logout: {}", currentSession.getSessionId());
            currentSession = null;
        }
        Main.switchScene("/fxml/Login.fxml");
    }

    @FXML
    private void onToggleTheme() {
        Main.isDarkMode = !Main.isDarkMode;
        Main.applyTheme(welcomeLabel.getScene());
        log.info("Theme toggled: {}", Main.isDarkMode ? "DARK" : "LIGHT");
    }
}
