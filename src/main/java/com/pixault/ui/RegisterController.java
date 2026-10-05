package com.pixault.ui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.pixault.Main;
import com.pixault.auth.AuthController;
import com.pixault.auth.AuthController.AuthException;
import com.pixault.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Registration screen controller.
 */
public class RegisterController {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmField;
    @FXML
    private Button registerBtn;
    @FXML
    private Label statusLabel;

    private final AuthController auth = new AuthController();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Timeline verificationPoller;
    private volatile boolean verificationCheckRunning = false;
    private String pendingVerificationEmail;

    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void onRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        char[] password = passwordField.getText().toCharArray();
        String confirm = confirmField.getText();

        if (username.isEmpty() || email.isEmpty() || password.length == 0) {
            showError("All fields are required.");
            return;
        }
        if (!passwordField.getText().equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (password.length < 12) {
            showError("Password must be at least 12 characters.");
            return;
        }

        registerBtn.setDisable(true);
        statusLabel.setText("Creating your vault account...");
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#38BDF8"));

        executor.submit(() -> {
            try {
                User user = auth.register(username, email, password);
                Arrays.fill(password, '\0');

                Platform.runLater(() -> {
                    VerifyEmailController.userToVerify = user;
                    Main.switchScene("/fxml/VerifyEmail.fxml");
                });
            } catch (AuthException e) {
                Arrays.fill(password, '\0');
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    registerBtn.setDisable(false);
                });
            } catch (SQLException e) {
                Arrays.fill(password, '\0');
                Platform.runLater(() -> {
                    showError("Database error occurred.");
                    registerBtn.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void onBackToLogin() {
        Main.switchScene("/fxml/Login.fxml");
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#EF4444"));
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#10B981"));
    }
}
