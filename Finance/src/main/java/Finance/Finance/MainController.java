package Finance.Finance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public class MainController {

    // Top bar
    @FXML private Label     topBarSubtitle, avatarLabel, userNameLabel, userRoleLabel;
    @FXML private TextField globalSearch;

    // Nav
    @FXML private Button navDashboard, navOverview, navExport, navAnomalies;

    // Pages
    @FXML private ScrollPane dashboardPage, overviewPage, exportPage, anomaliesPage;

    // KPIs
    @FXML private Label kpiRevenue,   kpiRevenueSub;
    @FXML private Label kpiNetIncome, kpiNetIncomeSub;
    @FXML private Label kpiRefunds,   kpiRefundsSub;
    @FXML private Label kpiPending,   kpiPendingSub;   // avg order value
    @FXML private Label kpiPending2;                   // outstanding payments
    @FXML private Label dashboardSubtitle;             // tax collected

    // Filters
    @FXML private DatePicker       filterDateFrom, filterDateTo;
    @FXML private ComboBox<String> filterCustomer, filterCategory;
    @FXML private Label            filterErrorLabel;

    // Chart + table
    @FXML private BarChart<String, Number>      revenueChart;
    @FXML private TableView<OrderRow>           ordersTable;
    // FIX: was TableColumn<OrderRow, Integer> — use String to match SimpleStringProperty cell factory
    @FXML private TableColumn<OrderRow, String> colOrderId;
    @FXML private TableColumn<OrderRow, String> colOrderCustomer, colOrderDate,
                                                 colOrderStatus, colOrderAmount;

    // Sub-controllers via fx:include
    @FXML private OverviewController overviewContentController;
    @FXML private ExportController   exportContentController;
    @FXML private AnomalyController  anomaliesContentController;

    private final NumberFormat GBP = NumberFormat.getCurrencyInstance(Locale.UK);

    // Called by LoginController after successful login
    public void initUser(String username, String fullName, String role) {
        safe(userNameLabel,  l -> l.setText(fullName));
        safe(userRoleLabel,  l -> l.setText(role));
        safe(avatarLabel,    l -> l.setText(fullName.substring(0, 1).toUpperCase()));
        safe(topBarSubtitle, l -> l.setText("Welcome back, " + fullName + " \u2022 " + role));
        loadDropdowns();
        refreshDashboard(null);
    }

    // ── Navigation ──────────────────────────────────────────────
    @FXML void showDashboard(ActionEvent e) { switchPage(dashboardPage, navDashboard); }
    @FXML void showOverview (ActionEvent e) { switchPage(overviewPage,  navOverview);  }
    @FXML void showExport   (ActionEvent e) { switchPage(exportPage,    navExport);    }
    @FXML void showAnomalies(ActionEvent e) { switchPage(anomaliesPage, navAnomalies); }

    private void switchPage(ScrollPane page, Button active) {
        for (ScrollPane p : new ScrollPane[]{dashboardPage, overviewPage, exportPage, anomaliesPage})
            p.setVisible(false);
        page.setVisible(true);

        for (Button b : new Button[]{navDashboard, navOverview, navExport, navAnomalies}) {
            b.getStyleClass().removeAll("nav-btn-active");
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        active.getStyleClass().add("nav-btn-active");
    }

    // ── Dashboard data ───────────────────────────────────────────
    @FXML
    public void refreshDashboard(ActionEvent e) {
        try {
            loadKPIs(null, null, null, null);
            loadRevenueChart(null, null);
            loadOrdersTable(null, null, null, null);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void applyFilters(ActionEvent e) {
        safe(filterErrorLabel, l -> l.setText(""));
        LocalDate from     = filterDateFrom != null ? filterDateFrom.getValue() : null;
        LocalDate to       = filterDateTo   != null ? filterDateTo.getValue()   : null;
        String    customer = filterCustomer != null ? filterCustomer.getValue() : null;
        String    category = filterCategory != null ? filterCategory.getValue() : null;

        if (from != null && to != null && from.isAfter(to)) {
            safe(filterErrorLabel, l -> l.setText("Invalid date range \u2014 Start Date must be before End Date."));
            return;
        }
        try {
            loadKPIs(from, to, customer, category);
            loadRevenueChart(from, to);
            loadOrdersTable(from, to, customer, category);
        } catch (Exception ex) {
            safe(filterErrorLabel, l -> l.setText("Filter error. Please check your inputs."));
            ex.printStackTrace();
        }
    }

    @FXML
    private void resetFilters(ActionEvent e) {
        if (filterDateFrom != null) filterDateFrom.setValue(null);
        if (filterDateTo   != null) filterDateTo.setValue(null);
        if (filterCustomer != null) filterCustomer.getSelectionModel().clearSelection();
        if (filterCategory != null) filterCategory.getSelectionModel().clearSelection();
        safe(filterErrorLabel, l -> l.setText(""));
        refreshDashboard(null);
    }

    private void loadKPIs(LocalDate from, LocalDate to,
                           String customer, String category) throws Exception {
        StringBuilder sb = new StringBuilder(
            "SELECT " +
            "  COALESCE(SUM(CASE WHEN o.status='COMPLETED' THEN o.total_amount ELSE 0 END),0) AS revenue, " +
            "  COALESCE(SUM(CASE WHEN o.status='REFUNDED'  THEN r.amount       ELSE 0 END),0) AS refunds, " +
            "  COALESCE(SUM(CASE WHEN o.status='PENDING'   THEN o.total_amount ELSE 0 END),0) AS pending, " +
            "  COUNT(CASE WHEN o.status='REFUNDED'  THEN 1 END) AS refund_count, " +
            "  COUNT(CASE WHEN o.status='COMPLETED' THEN 1 END) AS completed_count, " +
            "  COALESCE(ROUND(AVG(CASE WHEN o.status='COMPLETED' THEN o.total_amount END),2),0) AS avg_order " +
            "FROM orders o " +
            "LEFT JOIN refunds r      ON r.order_id    = o.id " +
            "LEFT JOIN customers c    ON c.id           = o.customer_id " +
            "LEFT JOIN order_items oi ON oi.order_id   = o.id " +
            "LEFT JOIN products p     ON p.id           = oi.product_id " +
            "LEFT JOIN categories cat ON cat.id          = p.category_id " +
            "WHERE 1=1");
        appendFilters(sb, from, to, customer, category);

        PreparedStatement ps = buildPs(sb.toString(), from, to, customer, category);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double refunds = rs.getDouble("refunds");
                double pending = rs.getDouble("pending");
                double avg     = rs.getDouble("avg_order");
                int    refCnt  = rs.getInt("refund_count");
                int    compCnt = rs.getInt("completed_count");
                double netInc  = revenue - refunds;
                double margin  = revenue > 0 ? (netInc / revenue * 100) : 0;
                double tax     = revenue * 0.20;

                safe(kpiRevenue,      l -> l.setText(GBP.format(revenue)));
                safe(kpiRevenueSub,   l -> l.setText(compCnt + " orders"));
                safe(kpiNetIncome,    l -> l.setText(GBP.format(netInc)));
                safe(kpiNetIncomeSub, l -> l.setText(String.format("%.1f%% margin", margin)));
                safe(kpiPending,      l -> l.setText(GBP.format(avg)));
                safe(kpiPendingSub,   l -> l.setText("Per completed order"));
                safe(kpiPending2,     l -> l.setText(GBP.format(pending)));
                safe(kpiRefunds,      l -> l.setText(GBP.format(refunds)));
                safe(kpiRefundsSub,   l -> l.setText(refCnt + " refunded orders"));
                safe(dashboardSubtitle, l -> l.setText(GBP.format(tax)));
            }
        }
    }

    // FIX: use PreparedStatement instead of string concatenation
    private void loadRevenueChart(LocalDate from, LocalDate to) throws Exception {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Revenue");

        StringBuilder sb = new StringBuilder(
            "SELECT strftime('%Y-%m', order_date) AS month, SUM(total_amount) AS total " +
            "FROM orders WHERE status='COMPLETED'");
        if (from != null) sb.append(" AND order_date >= ?");
        if (to   != null) sb.append(" AND order_date <= ?");
        sb.append(" GROUP BY month ORDER BY month LIMIT 12");

        PreparedStatement ps = DatabaseManager.getInstance().prepare(sb.toString());
        int i = 1;
        if (from != null) ps.setString(i++, from.toString());
        if (to   != null) ps.setString(i,   to + " 23:59:59");

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                s.getData().add(new XYChart.Data<>(rs.getString("month"), rs.getDouble("total")));
        }
        revenueChart.getData().add(s);
    }

    private void loadOrdersTable(LocalDate from, LocalDate to,
                                  String customer, String category) throws Exception {
        StringBuilder sb = new StringBuilder(
            "SELECT o.id, c.name AS cname, o.order_date, o.status, o.total_amount " +
            "FROM orders o JOIN customers c ON c.id = o.customer_id " +
            "LEFT JOIN order_items oi ON oi.order_id  = o.id " +
            "LEFT JOIN products p     ON p.id          = oi.product_id " +
            "LEFT JOIN categories cat ON cat.id         = p.category_id WHERE 1=1");
        appendFilters(sb, from, to, customer, category);
        sb.append(" GROUP BY o.id ORDER BY o.order_date DESC LIMIT 50");

        PreparedStatement ps = buildPs(sb.toString(), from, to, customer, category);

        // FIX: colOrderId now uses SimpleStringProperty to match TableColumn<OrderRow, String>
        colOrderId.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().id())));
        colOrderCustomer.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().customer()));
        colOrderDate.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().date().substring(0, 10)));
        colOrderStatus.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().status()));
        colOrderAmount.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().amount())));

        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new OrderRow(rs.getInt("id"), rs.getString("cname"),
                    rs.getString("order_date"), rs.getString("status"),
                    rs.getDouble("total_amount")));
        }
        ordersTable.setItems(rows);
        if (rows.isEmpty()) ordersTable.setPlaceholder(new Label("No orders found."));
    }

    private void loadDropdowns() {
        try {
            ObservableList<String> custs = FXCollections.observableArrayList((String) null);
            try (ResultSet rs = DatabaseManager.getInstance().query("SELECT name FROM customers ORDER BY name")) {
                while (rs.next()) custs.add(rs.getString(1));
            }
            if (filterCustomer != null) filterCustomer.setItems(custs);

            ObservableList<String> cats = FXCollections.observableArrayList((String) null);
            try (ResultSet rs = DatabaseManager.getInstance().query("SELECT name FROM categories ORDER BY name")) {
                while (rs.next()) cats.add(rs.getString(1));
            }
            if (filterCategory != null) filterCategory.setItems(cats);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void appendFilters(StringBuilder sb, LocalDate from, LocalDate to,
                                String customer, String category) {
        if (from     != null) sb.append(" AND o.order_date >= ?");
        if (to       != null) sb.append(" AND o.order_date <= ?");
        if (customer != null) sb.append(" AND c.name = ?");
        if (category != null) sb.append(" AND cat.name = ?");
    }

    private PreparedStatement buildPs(String sql, LocalDate from, LocalDate to,
                                       String customer, String category) throws Exception {
        PreparedStatement ps = DatabaseManager.getInstance().prepare(sql);
        int i = 1;
        if (from     != null) ps.setString(i++, from.toString());
        if (to       != null) ps.setString(i++, to + " 23:59:59");
        if (customer != null) ps.setString(i++, customer);
        if (category != null) ps.setString(i,   category);
        return ps;
    }

    @FXML
    private void handleLogout(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 860, 560));
            stage.setTitle("Pixel-Pocket Finance \u2014 Login");
            stage.show();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // Null-safe label setter
    private void safe(Label lbl, java.util.function.Consumer<Label> fn) {
        if (lbl != null) fn.accept(lbl);
    }
}