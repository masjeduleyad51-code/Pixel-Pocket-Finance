// Save as: src/main/java/Finance/Finance/AnomalyController.java
package Finance.Finance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AnomalyController {

    @FXML private Label anomalyStatusLabel, anomalyInfoLabel;
    @FXML private Label badgeOpen, badgeResolved, badgeHigh;

    @FXML private TableView<AnomalyRow>           anomalyTable;
    @FXML private TableColumn<AnomalyRow, String> colAnoId, colAnoDate, colAnoType,
                                                   colAnoDesc, colAnoSeverity, colAnoStatus;

    @FXML
    public void initialize() {
        // FIX: guard every field — initialize() fires during fx:include loading
        // before the parent controller is ready; null-checks prevent NPEs
        if (anomalyTable == null) return;
        setupTable();
        loadTable();
        updateBadges();
    }

    @FXML
    public void runAnalysis(ActionEvent e) {
        safeLabel(anomalyStatusLabel, "");
        safeLabel(anomalyInfoLabel,   "");
        try {
            int detected = detectAnomalies();
            safeLabel(anomalyInfoLabel, detected > 0
                ? detected + " new anomaly(s) detected."
                : "No new anomalies detected.");
            loadTable();
            updateBadges();
        } catch (Exception ex) {
            safeLabel(anomalyStatusLabel, "Not enough data to check anomalies.");
            ex.printStackTrace();
        }
    }

    @FXML
    public void markResolved(ActionEvent e) {
        if (anomalyTable == null) return;
        AnomalyRow selected = anomalyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            safeLabel(anomalyStatusLabel, "Please select a row to mark as resolved.");
            return;
        }
        try {
            try (PreparedStatement ps = DatabaseManager.getInstance()
                    .prepare("UPDATE anomaly_log SET is_resolved=1 WHERE id=?")) {
                ps.setInt(1, selected.id());
                ps.executeUpdate();
            }
            loadTable();
            updateBadges();
        } catch (Exception ex) {
            safeLabel(anomalyStatusLabel, "Could not update anomaly. Please try again.");
            ex.printStackTrace();
        }
    }

    // ── Anomaly detection ────────────────────────────────────────────
    private int detectAnomalies() throws Exception {
        int count = 0;

        // Check 1: refund rate > 20% in last 30 days
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT COUNT(*) AS total, " +
                "  SUM(CASE WHEN status='REFUNDED' THEN 1 ELSE 0 END) AS refunded " +
                "FROM orders WHERE order_date >= date('now','-30 days')")) {
            if (rs.next()) {
                int total    = rs.getInt("total");
                int refunded = rs.getInt("refunded");
                if (total > 0 && (refunded * 1.0 / total) > 0.20) {
                    logAnomaly("HIGH_REFUND_RATE",
                        "Refund rate exceeded 20% in the last 30 days (" + refunded + "/" + total + " orders)",
                        "HIGH");
                    count++;
                }
            }
        }

        // Check 2: no completed orders in last 7 days
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT COUNT(*) AS cnt FROM orders " +
                "WHERE status='COMPLETED' AND order_date >= date('now','-7 days')")) {
            if (rs.next() && rs.getInt("cnt") == 0) {
                logAnomaly("LOW_SALES",
                    "No completed sales recorded in the last 7 days.", "MEDIUM");
                count++;
            }
        }

        // Check 3: any product with > 2 refunds
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT p.name, COUNT(r.id) AS rc " +
                "FROM refunds r " +
                "JOIN order_items oi ON oi.order_id = r.order_id " +
                "JOIN products p     ON p.id = oi.product_id " +
                "GROUP BY p.id HAVING rc > 2")) {
            while (rs.next()) {
                logAnomaly("HIGH_REFUND_RATE",
                    "Product '" + rs.getString("name") + "' has " + rs.getInt("rc") + " refunds.",
                    "HIGH");
                count++;
            }
        }

        return count;
    }

    private void logAnomaly(String type, String desc, String severity) throws Exception {
        // Skip if an identical open anomaly already exists
        try (PreparedStatement check = DatabaseManager.getInstance().prepare(
                "SELECT COUNT(*) FROM anomaly_log " +
                "WHERE anomaly_type=? AND description=? AND is_resolved=0")) {
            check.setString(1, type);
            check.setString(2, desc);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }

        try (PreparedStatement ps = DatabaseManager.getInstance().prepare(
                "INSERT INTO anomaly_log (anomaly_type, description, severity) VALUES (?,?,?)")) {
            ps.setString(1, type);
            ps.setString(2, desc);
            ps.setString(3, severity);
            ps.executeUpdate();
        }
    }

    // ── Table helpers ─────────────────────────────────────────────────
    private void setupTable() {
        colAnoId      .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().id())));
        colAnoDate    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().detectedAt()));
        colAnoType    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().type()));
        colAnoDesc    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().description()));
        colAnoSeverity.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().severity()));
        colAnoStatus  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().resolved() == 1 ? "Resolved" : "Open"));
    }

    private void loadTable() {
        if (anomalyTable == null) return;
        try (ResultSet rs = DatabaseManager.getInstance()
                .query("SELECT * FROM anomaly_log ORDER BY detected_at DESC")) {
            ObservableList<AnomalyRow> rows = FXCollections.observableArrayList();
            while (rs.next()) {
                rows.add(new AnomalyRow(
                    rs.getInt("id"),              rs.getString("detected_at"),
                    rs.getString("anomaly_type"), rs.getString("description"),
                    rs.getString("severity"),     rs.getInt("is_resolved")
                ));
            }
            anomalyTable.setItems(rows);
        } catch (Exception e) {
            safeLabel(anomalyStatusLabel, "Could not load anomaly data.");
            e.printStackTrace();
        }
    }

    private void updateBadges() {
        if (badgeOpen == null) return;
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT " +
                "  SUM(CASE WHEN is_resolved=0 THEN 1 ELSE 0 END) AS open_cnt, " +
                "  SUM(CASE WHEN is_resolved=1 THEN 1 ELSE 0 END) AS res_cnt, " +
                "  SUM(CASE WHEN severity='HIGH' AND is_resolved=0 THEN 1 ELSE 0 END) AS high_cnt " +
                "FROM anomaly_log")) {
            if (rs.next()) {
                badgeOpen    .setText(String.valueOf(rs.getInt("open_cnt")));
                badgeResolved.setText(String.valueOf(rs.getInt("res_cnt")));
                badgeHigh    .setText(String.valueOf(rs.getInt("high_cnt")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void safeLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }
}