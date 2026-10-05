package com.pixault.ui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import com.pixault.Main;
import com.pixault.auth.AuthController;
import com.pixault.auth.AuthController.AuthException;
import com.pixault.database.UserDAO;
import com.pixault.model.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings screen controller.
 * Supports: change password.
 */
public class SettingsController {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button changePasswordBtn;
    @FXML
    private Label pwStatusLabel;

    @FXML
    private Label sessionInfoLabel;
    @FXML
    private Label deviceLabel;

    private final AuthController auth = new AuthController();
    private final UserDAO userDAO = new UserDAO();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        pwStatusLabel.setText("");
        Session s = DashboardController.currentSession;
        if (s != null) {
            sessionInfoLabel.setText("Logged in as: " + s.getUsername() + " (ID: " + s.getUserId() + ")");
            deviceLabel.setText("Device: " + s.getDeviceFingerprint().substring(0, 8).toUpperCase() + "...");
        }
    }

    @FXML
    private void onChangePassword() {
        char[] current = currentPasswordField.getText().toCharArray();
        char[] newPw = newPasswordField.getText().toCharArray();
        String confirm = confirmPasswordField.getText();

        if (current.length == 0 || newPw.length == 0) {
            showPwError("All password fields are required.");
            return;
        }
        if (!newPasswordField.getText().equals(confirm)) {
            showPwError("New passwords do not match.");
            return;
        }
        if (newPw.length < 12) {
            showPwError("New password must be at least 12 characters.");
            return;
        }

        changePasswordBtn.setDisable(true);
        showPwInfo("Changing password...");

        executor.submit(() -> {
            try {
                Session s = DashboardController.currentSession;
                if (s == null) {
                    throw new RuntimeException("No active session.");
                }
                Optional<com.pixault.model.User> optUser = userDAO.findById(s.getUserId());
                if (optUser.isEmpty()) {
                    throw new RuntimeException("User not found.");
                }

                auth.changePassword(optUser.get(), current, newPw);
                java.util.Arrays.fill(current, '\0');
                java.util.Arrays.fill(newPw, '\0');

                Platform.runLater(() -> {
                    showPwSuccess("Password changed. All sessions invalidated. Please log in again.");
                    com.pixault.auth.SessionManager.invalidateAllSessions(s.getUserId());
                    DashboardController.currentSession = null;
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(() -> Main.switchScene("/fxml/Login.fxml"));
                    }).start();
                });
            } catch (AuthException e) {
                Platform.runLater(() -> {
                    showPwError(e.getMessage());
                    changePasswordBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showPwError("Error: " + e.getMessage());
                    changePasswordBtn.setDisable(false);
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

    private void showPwError(String m) {
        pwStatusLabel.setText(m);
        pwStatusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void showPwSuccess(String m) {
        pwStatusLabel.setText(m);
        pwStatusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void showPwInfo(String m) {
        pwStatusLabel.setText(m);
        pwStatusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }
}
