// Save as: src/main/java/Finance/Finance/ExportRow.java
package Finance.Finance;

public record ExportRow(
    int    orderId,
    String date,
    String customer,
    String product,
    String category,
    int    qty,
    double revenue,
    double cost,
    String status
) {}
