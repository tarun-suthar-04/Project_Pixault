package com.pixault.ui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.pixault.Main;
import com.pixault.auth.AuthController;
import com.pixault.auth.AuthController.AuthException;
import com.pixault.model.Session;
import com.pixault.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Login screen controller - orchestrates password plus OTP authentication.
 */
public class LoginController {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginBtn;
    @FXML
    private Label statusLabel;

    private final AuthController auth = new AuthController();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Current MFA step: 1=password, 2=otp */
    private int mfaStep = 1;
    private User authenticatedUser;

    @FXML
    public void initialize() {
        statusLabel.setText("");
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onLogin();
            }
        });
    }

    @FXML
    private void onLogin() {
        String identifier = usernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();

        if (identifier.isEmpty() || password.length == 0) {
            showError("Please enter username/email and password.");
            return;
        }

        setLoading(true, "Authenticating...");
        executor.submit(() -> {
            try {
                User user = auth.authenticatePassword(identifier, password);
                Arrays.fill(password, '\0');

                try {
                    Session session = auth.loginSuccess(user);
                    Platform.runLater(() -> {
                        DashboardController.currentSession = session;
                        Main.switchScene("/fxml/Dashboard.fxml");
                    });
                } catch (Exception e) {
                    log.error("Failed to process login success", e);
                    Platform.runLater(() -> {
                        showError("Error completing login.");
                        setLoading(false, null);
                    });
                }
            } catch (AuthException e) {
                Arrays.fill(password, '\0');
                if ("EMAIL_UNVERIFIED".equals(e.getMessage())) {
                    try {
                        // Re-fetch user to initiate verification (since we don't have the user object here)
                        User unverifiedUser = new com.pixault.database.UserDAO().findByUsernameOrEmail(identifier).orElseThrow();
                        auth.initiateEmailVerification(unverifiedUser);
                        Platform.runLater(() -> {
                            VerifyEmailController.userToVerify = unverifiedUser;
                            Main.switchScene("/fxml/VerifyEmail.fxml");
                        });
                    } catch (Exception ex) {
                        log.error("Failed to handle unverified user", ex);
                        Platform.runLater(() -> {
                            showError("Could not send verification email.");
                            setLoading(false, null);
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        showError(e.getMessage());
                        setLoading(false, null);
                    });
                }
            } catch (SQLException e) {
                Arrays.fill(password, '\0');
                log.error("Database error during login", e);
                Platform.runLater(() -> {
                    showError("Database error occurred.");
                    setLoading(false, null);
                });
            } catch (Throwable t) {
                Arrays.fill(password, '\0');
                log.error("Unexpected error during login", t);
                Platform.runLater(() -> {
                    showError("Error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                    setLoading(false, null);
                });
            }
        });
    }

    @FXML
    private void onRegister() {
        Main.switchScene("/fxml/Register.fxml");
    }

    @FXML
    private void onForgotPassword() {
        Main.switchScene("/fxml/ForgotPassword.fxml");
    }


    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
    }

    private void setLoading(boolean loading, String msg) {
        loginBtn.setDisable(loading);
        if (loading && msg != null) {
            statusLabel.setText(msg);
        }
    }
}
