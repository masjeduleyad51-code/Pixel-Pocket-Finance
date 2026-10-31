// Save as: src/main/java/Finance/Finance/OverviewController.java
package Finance.Finance;

import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;

public class OverviewController {

    @FXML private Label ovAvgOrder, ovTopProduct, ovCustomers, ovRefundRate;
    @FXML private Label overviewErrorLabel;

    @FXML private PieChart                  categoryPieChart;
    @FXML private LineChart<String, Number> trendLineChart;

    @FXML private TableView<ProductProfitRow>           profitTable;
    @FXML private TableColumn<ProductProfitRow, String> colProdName, colProdCategory,
                                                         colProdCost, colProdPrice,
                                                         colProdProfit, colProdMargin,
                                                         colProdSold, colProdTotal;
    @FXML private Label profitErrorLabel;

    private final NumberFormat GBP = NumberFormat.getCurrencyInstance(Locale.UK);

    @FXML
    public void initialize() {
        refreshOverview(null);
    }

    @FXML
    public void refreshOverview(ActionEvent e) {
        overviewErrorLabel.setText("");
        try {
            loadSummaryCards();
            loadCategoryPie();
            loadTrendLine();
            loadProfitTable();
        } catch (Exception ex) {
            overviewErrorLabel.setText("Analytics data unavailable. Please try again later.");
            ex.printStackTrace();
        }
    }

    private void loadSummaryCards() throws Exception {
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT " +
                "  ROUND(AVG(total_amount),2) AS avg_order, " +
                "  COUNT(DISTINCT customer_id) AS cust_count, " +
                "  SUM(CASE WHEN status='REFUNDED' THEN 1 ELSE 0 END)*1.0 / COUNT(*) * 100 AS refund_rate " +
                "FROM orders")) {
            if (rs.next()) {
                ovAvgOrder  .setText(GBP.format(rs.getDouble("avg_order")));
                // FIX: label correctly shows total unique customers (not "ONLINE" count)
                ovCustomers .setText(rs.getInt("cust_count") + " customers");
                ovRefundRate.setText(String.format("%.1f%%", rs.getDouble("refund_rate")));
            }
        }

        try (ResultSet rp = DatabaseManager.getInstance().query(
                "SELECT product_name FROM v_product_profit ORDER BY total_profit DESC LIMIT 1")) {
            ovTopProduct.setText(rp.next() ? rp.getString(1) : "\u2014");
        }
    }

    private void loadCategoryPie() throws Exception {
        categoryPieChart.getData().clear();
        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT cat.name, COALESCE(SUM(oi.quantity * oi.unit_price),0) AS rev " +
                "FROM categories cat " +
                "LEFT JOIN products p     ON p.category_id = cat.id " +
                "LEFT JOIN order_items oi ON oi.product_id = p.id " +
                "LEFT JOIN orders o       ON o.id = oi.order_id AND o.status='COMPLETED' " +
                "GROUP BY cat.name ORDER BY rev DESC")) {
            while (rs.next()) {
                double rev = rs.getDouble("rev");
                if (rev > 0) slices.add(new PieChart.Data(rs.getString("name"), rev));
            }
        }
        categoryPieChart.setData(slices);
    }

    private void loadTrendLine() throws Exception {
        trendLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        try (ResultSet rs = DatabaseManager.getInstance().query(
                "SELECT strftime('%Y-%m', order_date) AS month, SUM(total_amount) AS total " +
                "FROM orders WHERE status='COMPLETED' " +
                "GROUP BY month ORDER BY month DESC LIMIT 6")) {
            while (rs.next()) {
                series.getData().add(
                    new XYChart.Data<>(rs.getString("month"), rs.getDouble("total")));
            }
        }
        trendLineChart.getData().add(series);
    }

    // R4: Product profit & margin analysis
    private void loadProfitTable() throws Exception {
        colProdName    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name()));
        colProdCategory.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().category()));
        colProdCost    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().cost())));
        colProdPrice   .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().price())));
        colProdProfit  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().profitPerUnit())));
        colProdMargin  .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            String.format("%.1f%%", d.getValue().marginPct())));
        colProdSold    .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(d.getValue().unitsSold())));
        colProdTotal   .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(GBP.format(d.getValue().totalProfit())));

        ObservableList<ProductProfitRow> rows = FXCollections.observableArrayList();
        try (ResultSet rs = DatabaseManager.getInstance()
                .query("SELECT * FROM v_product_profit ORDER BY total_profit DESC")) {
            while (rs.next()) {
                rows.add(new ProductProfitRow(
                    rs.getString("product_name"), rs.getString("category"),
                    rs.getDouble("cost_price"),   rs.getDouble("sale_price"),
                    rs.getDouble("profit_per_unit"), rs.getDouble("margin_pct"),
                    rs.getInt("units_sold"),        rs.getDouble("total_profit")
                ));
            }
        }
        profitTable.setItems(rows);

        if (rows.isEmpty()) {
            profitErrorLabel.setText("Cannot compute profit. Missing product data.");
        } else {
            profitErrorLabel.setText("");
        }
    }
}