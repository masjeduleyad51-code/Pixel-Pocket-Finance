// ── OrderRow.java ─────────────────────────────────────────────────
// Save as: src/main/java/Finance/Finance/OrderRow.java
package Finance.Finance;

public record OrderRow(
    int    id,
    String customer,
    String date,
    String status,
    double amount
) {}
