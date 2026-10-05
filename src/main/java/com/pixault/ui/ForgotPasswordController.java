package com.pixault.ui;

import com.pixault.Main;
import com.pixault.auth.AuthController;
import com.pixault.auth.AuthController.AuthException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML private VBox step1Box, step2Box, step3Box;
    @FXML private TextField emailField, otpField;
    @FXML private PasswordField newPasswordField, confirmPasswordField;
    @FXML private Label statusLabel1, statusLabel2, statusLabel3;
    @FXML private Label otpSentLabel;

    private final AuthController auth = new AuthController();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        showStep(1);
    }

    @FXML
    private void onRequestReset() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError(1, "Please enter your email.");
            return;
        }

        statusLabel1.setText("Processing...");
        executor.submit(() -> {
            try {
                auth.initiateForgotPassword(email);
                Platform.runLater(() -> {
                    otpSentLabel.setText("Recovery code sent to: " + email);
                    showStep(2);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(1, "An error occurred."));
            }
        });
    }

    @FXML
    private void onVerifyOtp() {
        String otp = otpField.getText().trim();
        if (otp.length() != 6) {
            showError(2, "Enter the 6-digit code.");
            return;
        }
        showStep(3);
    }

    @FXML
    private void onResetPassword() {
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        char[] newPwd = newPasswordField.getText().toCharArray();
        char[] confirmPwd = confirmPasswordField.getText().toCharArray();

        if (newPwd.length < 12) {
            showError(3, "Password must be at least 12 characters.");
            return;
        }
        if (!Arrays.equals(newPwd, confirmPwd)) {
            showError(3, "Passwords do not match.");
            return;
        }

        statusLabel3.setText("Updating password...");
        executor.submit(() -> {
            try {
                auth.resetPasswordWithOtp(email, otp, newPwd);
                Arrays.fill(newPwd, '\0');
                Arrays.fill(confirmPwd, '\0');
                
                Platform.runLater(() -> {
                    statusLabel3.setStyle("-fx-text-fill: #10B981;");
                    statusLabel3.setText("Password reset successful! Redirecting...");
                    executor.submit(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> Main.switchScene("/fxml/Login.fxml"));
                    });
                });
            } catch (AuthException e) {
                Arrays.fill(newPwd, '\0');
                Arrays.fill(confirmPwd, '\0');
                Platform.runLater(() -> showError(3, e.getMessage()));
            } catch (Exception e) {
                Arrays.fill(newPwd, '\0');
                Arrays.fill(confirmPwd, '\0');
                Platform.runLater(() -> showError(3, "Failed to reset password."));
            }
        });
    }

    @FXML
    private void onBackToLogin() {
        Main.switchScene("/fxml/Login.fxml");
    }

    private void showStep(int step) {
        step1Box.setVisible(step == 1);
        step2Box.setVisible(step == 2);
        step3Box.setVisible(step == 3);
        
        step1Box.setManaged(step == 1);
        step2Box.setManaged(step == 2);
        step3Box.setManaged(step == 3);
    }

    private void showError(int step, String msg) {
        Label label = switch (step) {
            case 1 -> statusLabel1;
            case 2 -> statusLabel2;
            case 3 -> statusLabel3;
            default -> statusLabel1;
        };
        label.setText(msg);
        label.setStyle("-fx-text-fill: #EF4444;");
    }
}
