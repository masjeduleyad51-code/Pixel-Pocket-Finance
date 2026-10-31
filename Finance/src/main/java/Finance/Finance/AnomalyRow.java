package Finance.Finance;

// Save as: src/main/java/Finance/Finance/AnomalyRow.java


public record AnomalyRow(
    int    id,
    String detectedAt,
    String type,
    String description,
    String severity,
    int    resolved
) {}