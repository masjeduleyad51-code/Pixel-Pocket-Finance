package Finance.Finance;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        errorLabel.setText("");

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Login Failed. Please check your credentials.");
            return;
        }

        try {
            PreparedStatement ps = DatabaseManager.getInstance().prepare(
                "SELECT id, role, is_active, full_name FROM users " +
                "WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                errorLabel.setText("Login Failed. Please check your credentials.");
                return;
            }

            String role     = rs.getString("role");
            int    isActive = rs.getInt("is_active");
            String fullName = rs.getString("full_name");
            int    userId   = rs.getInt("id");

            if (isActive != 1 || (!role.equals("ADMIN") && !role.equals("FINANCE"))) {
                errorLabel.setText("Access Denied. Finance/Admin privileges required.");
                return;
            }

            PreparedStatement upd = DatabaseManager.getInstance()
                .prepare("UPDATE users SET last_login = datetime('now') WHERE id = ?");
            upd.setInt(1, userId);
            upd.executeUpdate();

            openMainWindow(username, fullName, role);

        } catch (Exception e) {
            errorLabel.setText("Login Failed. Please check your credentials.");
            e.printStackTrace();
        }
    }

    private void openMainWindow(String username, String fullName, String role) {
        try {
            // Main.fxml is at src/main/resources/Main.fxml (flat root)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
            Parent root = loader.load();

            MainController ctrl = loader.getController();
            ctrl.initUser(username, fullName, role);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
            stage.setTitle("Pixel-Pocket Finance – " + fullName);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) {
            errorLabel.setText("Login Failed. Please check your credentials.");
            e.printStackTrace();
        }
    }
}
