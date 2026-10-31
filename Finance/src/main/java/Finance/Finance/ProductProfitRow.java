// Save as: src/main/java/Finance/Finance/ProductProfitRow.java
package Finance.Finance;

public record ProductProfitRow(
    String name,
    String category,
    double cost,
    double price,
    double profitPerUnit,
    double marginPct,
    int    unitsSold,
    double totalProfit
) {}
