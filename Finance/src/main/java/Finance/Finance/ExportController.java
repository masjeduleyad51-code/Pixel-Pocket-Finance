// Save as: src/main/java/Finance/Finance/ExportController.java
package Finance.Finance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportController {

    @FXML private DatePicker       exportDateFrom, exportDateTo;
    @FXML private ComboBox<String> exportCustomer, exportCategory;
    @FXML private RadioButton      radioCSV, radioPDF;
    @FXML private Label            exportFilterError, exportSuccessLabel, exportFailLabel;
    @FXML private ProgressBar      exportProgress;
    @FXML private HBox             progressRow;
    @FXML private TableView<ExportRow>           previewTable;
    @FXML private TableColumn<ExportRow, String> colExpOrderId, colExpDate, colExpCustomer,
                                                  colExpProduct, colExpCategory, colExpQty,
                                                  colExpRevenue, colExpCost, colExpProfit,
                                                  colExpStatus;
    @FXML private Label previewEmptyLabel;

    private List<ExportRow> currentRows = new ArrayList<>();
    private final NumberFormat GBP = NumberFormat.getCurrencyInstance(Locale.UK);

    @FXML
    public void initialize() {
        // FIX: clear the success label on load so it doesn't show stale text
        exportSuccessLabel.setText("");

        try {
            ObservableList<String> customers = FXCollections.observableArrayList((String) null);
            try (ResultSet rs = DatabaseManager.getInstance()
                    .query("SELECT name FROM customers ORDER BY name")) {
                while (rs.next()) customers.add(rs.getString(1));
            }
            exportCustomer.setItems(customers);

            ObservableList<String> cats = FXCollections.observableArrayList((String) null);
            try (ResultSet rs = DatabaseManager.getInstance()
                    .query("SELECT name FROM categories ORDER BY name")) {
                while (rs.next()) cats.add(rs.getString(1));
            }
            exportCategory.setItems(cats);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ToggleGroup tg = new ToggleGroup();
        radioCSV.setToggleGroup(tg);
        radioPDF.setToggleGroup(tg);
    }

    @FXML
    public void resetExport(ActionEvent e) {
        if (exportDateFrom != null) exportDateFrom.setValue(null);
        if (exportDateTo   != null) exportDateTo.setValue(null);
        if (exportCustomer != null) exportCustomer.getSelectionModel().clearSelection();
        if (exportCategory != null) exportCategory.getSelectionModel().clearSelection();
        clearMessages();
        currentRows.clear();
        previewTable.setItems(FXCollections.observableArrayList());
        previewEmptyLabel.setText("Apply filters to preview the report.");
    }

    @FXML
    public void previewReport(ActionEvent e) {
        clearMessages();
        LocalDate from     = exportDateFrom.getValue();
        LocalDate to       = exportDateTo.getValue();
        String    customer = exportCustomer.getValue();
        String    category = exportCategory.getValue();

        if (from != null && to != null && from.isAfter(to)) {
            exportFilterError.setText("Invalid date range — Start Date must be before End Date.");
            return;
        }

        try {
            currentRows = queryExportData(from, to, customer, category);
            populatePreviewTable(currentRows);
            previewEmptyLabel.setText(currentRows.isEmpty()
                ? "No data matches the selected filters."
                : "");
        } catch (Exception ex) {
            exportFilterError.setText("Failed to load data. Please check your filters.");
            ex.printStackTrace();
        }
    }

    @FXML
    public void exportReport(ActionEvent e) {
        clearMessages();

        // FIX: guard with a clear message — user must run Apply Filters first
        if (currentRows.isEmpty()) {
            exportFailLabel.setText("No data to export. Apply filters and preview first.");
            return;
        }

        boolean isPDF = radioPDF.isSelected();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report");
        // FIX: PDF silently saves as CSV — tell the user via the extension label
        if (isPDF) {
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV File (PDF not yet supported)", "*.csv"));
            chooser.setInitialFileName("finance_report.csv");
        } else {
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            chooser.setInitialFileName("finance_report.csv");
        }

        File file = chooser.showSaveDialog(previewTable.getScene().getWindow());
        if (file == null) return;

        progressRow.setVisible(true);
        progressRow.setManaged(true);
        exportProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exportCSV(file);   // PDF falls back to CSV until PDFBox is integrated
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            progressRow.setVisible(false);
            progressRow.setManaged(false);
            exportProgress.setProgress(0);
            exportSuccessLabel.setText("✓ Report saved: " + file.getAbsolutePath());
        });
        task.setOnFailed(ev -> {
            progressRow.setVisible(false);
            progressRow.setManaged(false);
            exportFailLabel.setText("Export failed. Please try again.");
            task.getException().printStackTrace();
        });
        new Thread(task, "export-thread").start();
    }

    // R6: CSV export
    private void exportCSV(File file) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Order #,Date,Customer,Product,Category,Qty,Revenue,Cost,Profit,Status");
            for (ExportRow r : currentRows) {
                pw.printf("%d,%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%s%n",
                    r.orderId(), r.date(), r.customer(), r.product(),
                    r.category(), r.qty(), r.revenue(), r.cost(),
                    (r.revenue() - r.cost()), r.status());
            }
        }
    }

    // FIX: use PreparedStatement — no string concatenation / SQL injection risk
    private List<ExportRow> queryExportData(LocalDate from, LocalDate to,
                                             String customer, String category) throws Exception {
        StringBuilder sb = new StringBuilder(
            "SELECT o.id, o.order_date, c.name AS cname, p.name AS pname, " +
            "       cat.name AS catname, oi.quantity, " +
            "       oi.quantity * oi.unit_price AS revenue, " +
            "       oi.quantity * oi.unit_cost  AS cost, " +
            "       o.status " +
            "FROM order_items oi " +
            "JOIN orders o       ON o.id   = oi.order_id " +
            "JOIN customers c    ON c.id   = o.customer_id " +
            "JOIN products p     ON p.id   = oi.product_id " +
            "JOIN categories cat ON cat.id = p.category_id " +
            "WHERE 1=1"
        );
        if (from     != null) sb.append(" AND o.order_date >= ?");
        if (to       != null) sb.append(" AND o.order_date <= ?");
        if (customer != null) sb.append(" AND c.name = ?");
        if (category != null) sb.append(" AND cat.name = ?");
        sb.append(" ORDER BY o.order_date DESC");

        PreparedStatement ps = DatabaseManager.getInstance().prepare(sb.toString());
        int i = 1;
        if (from     != null) ps.setString(i++, from.toString());
        if (to       != null) ps.setString(i++, to + " 23:59:59");
        if (customer != null) ps.setString(i++, customer);
        if (category != null) ps.setString(i,   category);

        List<ExportRow> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ExportRow(
                    rs.getInt("id"),           rs.getString("order_date"),
                    rs.getString("cname"),     rs.getString("pname"),
                    rs.getString("catname"),   rs.getInt("quantity"),
                    rs.getDouble("revenue"),   rs.getDouble("cost"),
                    rs.getString("status")
                ));
            }
        }
        return list;
    }

    private void populatePreviewTable(List<ExportRow> rows) {
        colExpOrderId .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().orderId())));
        colExpDate    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().date().substring(0, 10)));
        colExpCustomer.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().customer()));
        colExpProduct .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().product()));
        colExpCategory.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().category()));
        colExpQty     .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().qty())));
        colExpRevenue .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().revenue())));
        colExpCost    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().cost())));
        colExpProfit  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().revenue() - d.getValue().cost())));
        colExpStatus  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status()));
        previewTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void clearMessages() {
        exportFilterError.setText("");
        exportSuccessLabel.setText("");
        exportFailLabel.setText("");
        previewEmptyLabel.setText("");
    }
}