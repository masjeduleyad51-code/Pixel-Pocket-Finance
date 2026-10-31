package Finance.Finance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // FXML is at src/main/resources/Login.fxml (flat resources root)
        // getResource("/Login.fxml") resolves to the classpath root
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 560);
        stage.setTitle("Pixel-Pocket Finance — Login");
        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(440);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}