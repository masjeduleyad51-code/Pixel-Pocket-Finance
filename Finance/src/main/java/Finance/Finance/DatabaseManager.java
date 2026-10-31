package Finance.Finance;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger LOG     = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_PATH = "Database.db";   // matches your actual file
    private static final String URL     = "jdbc:sqlite:" + DB_PATH;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);

            Statement st = connection.createStatement();

            // ── KEY FIX: wait up to 5 seconds when DB is locked ──────────
            // Instead of throwing SQLITE_BUSY immediately, SQLite will
            // retry the write for 5000 ms before giving up.
            st.execute("PRAGMA busy_timeout = 5000;");

            // Enable WAL mode — allows readers and one writer simultaneously,
            // greatly reduces locking conflicts with DB Browser / other tools.
            st.execute("PRAGMA journal_mode = WAL;");

            st.execute("PRAGMA foreign_keys = ON;");

            LOG.info("SQLite connected: " + DB_PATH);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to connect to database", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement prepare(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public ResultSet query(String sql) throws SQLException {
        return connection.createStatement().executeQuery(sql);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("SQLite connection closed.");
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error closing DB connection", e);
        }
    }
}