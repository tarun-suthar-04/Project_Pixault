package com.pixault.ui;

import com.pixault.Main;
import com.pixault.database.ShareDAO;
import com.pixault.model.AuditLog;
import com.pixault.model.Session;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audit Log viewer controller.
 * Displays all security events for the current user.
 */
public class AuditLogController {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private TableView<AuditLog> auditTable;
    @FXML
    private TableColumn<AuditLog, String> colTimestamp;
    @FXML
    private TableColumn<AuditLog, String> colEvent;
    @FXML
    private TableColumn<AuditLog, String> colDevice;
    @FXML
    private TableColumn<AuditLog, String> colDetails;
    @FXML
    private TextField searchField;
    @FXML
    private Label statusLabel;

    private final ShareDAO shareDAO = new ShareDAO();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<AuditLog> allLogs;

    @FXML
    public void initialize() {
        // Lock columns to table width — no overflow, no user resizing
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colTimestamp.setResizable(false);
        colEvent.setResizable(false);
        colDevice.setResizable(false);
        colDetails.setResizable(false);

        colTimestamp.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getTimestamp().format(FMT)));
        colEvent.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getEventType()));
        colDevice.setCellValueFactory(d -> {
            String dev = d.getValue().getDeviceInfo();
            String short_ = (dev != null && dev.length() >= 8) ? dev.substring(0, 8) + "…" : dev;
            return new javafx.beans.property.SimpleStringProperty(short_);
        });
        colDetails.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDetails()));

        searchField.textProperty().addListener((obs, old, val) -> filterLogs(val));
        loadLogs();
    }

    private void loadLogs() {
        Session s = DashboardController.currentSession;
        if (s == null)
            return;
        statusLabel.setText("Loading audit logs…");
        executor.submit(() -> {
            try {
                allLogs = shareDAO.findAuditLogs(s.getUserId(), 500);
                Platform.runLater(() -> {
                    auditTable.setItems(FXCollections.observableArrayList(allLogs));
                    statusLabel.setText("Showing " + allLogs.size() + " events.");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> statusLabel.setText("Error loading logs: " + e.getMessage()));
            }
        });
    }

    private void filterLogs(String query) {
        if (allLogs == null)
            return;
        if (query == null || query.isBlank()) {
            auditTable.setItems(FXCollections.observableArrayList(allLogs));
            return;
        }
        String q = query.toLowerCase();
        List<AuditLog> filtered = allLogs.stream()
                .filter(a -> a.getEventType().toLowerCase().contains(q)
                        || (a.getDetails() != null && a.getDetails().toLowerCase().contains(q)))
                .toList();
        auditTable.setItems(FXCollections.observableArrayList(filtered));
        statusLabel.setText("Showing " + filtered.size() + " of " + allLogs.size() + " events.");
    }

    @FXML
    private void onRefresh() {
        loadLogs();
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
}
