package com.pixault.ui;

import com.pixault.Main;
import com.pixault.auth.AuthController;
import com.pixault.auth.AuthController.AuthException;
import com.pixault.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerifyEmailController {

    private static final Logger log = LoggerFactory.getLogger(VerifyEmailController.class);
    
    public static User userToVerify;

    @FXML private Label emailLabel;
    @FXML private TextField otpField;
    @FXML private Label statusLabel;
    @FXML private Button verifyBtn;

    private final AuthController auth = new AuthController();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        if (userToVerify != null) {
            emailLabel.setText("Verification code sent to: " + userToVerify.getEmail());
        }
        statusLabel.setText("");
        
        // Auto-focus and numeric filter
        Platform.runLater(() -> otpField.requestFocus());
        otpField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                otpField.setText(val.replaceAll("[^\\d]", ""));
            }
            if (val.length() > 6) {
                otpField.setText(val.substring(0, 6));
            }
        });
    }

    @FXML
    private void onVerify() {
        String otp = otpField.getText().trim();
        if (otp.length() != 6) {
            showError("Enter the 6-digit code.");
            return;
        }

        setLoading(true, "Verifying code...");
        executor.submit(() -> {
            try {
                auth.verifyEmailOtp(userToVerify, otp);
                Platform.runLater(() -> {
                    showSuccess("Email verified successfully! You can now login.");
                    executor.submit(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> Main.switchScene("/fxml/Login.fxml"));
                    });
                });
            } catch (AuthException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    setLoading(false, null);
                });
            } catch (Exception e) {
                log.error("Verification error", e);
                Platform.runLater(() -> {
                    showError("An error occurred. Please try again.");
                    setLoading(false, null);
                });
            }
        });
    }

    @FXML
    private void onResend() {
        setLoading(true, "Resending code...");
        executor.submit(() -> {
            try {
                auth.initiateEmailVerification(userToVerify);
                Platform.runLater(() -> {
                    showSuccess("New code sent to your email.");
                    setLoading(false, null);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to resend code. Please try again.");
                    setLoading(false, null);
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
        statusLabel.setStyle("-fx-text-fill: #EF4444;");
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #10B981;");
    }

    private void setLoading(boolean loading, String msg) {
        verifyBtn.setDisable(loading);
        if (loading && msg != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: #38BDF8;");
        }
    }
}
