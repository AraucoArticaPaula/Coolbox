package coolbox.sistema;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            // Cargar la fuente de iconos (Font Awesome) y Roboto antes de mostrar cualquier vista
            Font.loadFont(getClass().getResourceAsStream("/coolbox/sistema/fonts/fa-solid-900.ttf"), 14);
            Font.loadFont(getClass().getResourceAsStream("/coolbox/sistema/fonts/Roboto-Medium.ttf"), 14);

            Parent root = FXMLLoader.load(getClass().getResource("/coolbox/sistema/Vistas/Login.fxml"));
            Scene scene = new Scene(root);
            stage.setTitle("Coolbox - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("CAUSA: " + (e.getCause() != null ? e.getCause().getMessage() : "ninguna"));
            throw e;
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}