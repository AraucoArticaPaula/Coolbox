package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalOlvidePasswordController {

    @FXML
    private TextField txtCorreo;

    @FXML
    private Label lblErrorCorreo;

    @FXML
    private HBox paneExito;

    @FXML
    private Button btnEnviar;

    @FXML
    private void onCerrar(ActionEvent event) {
        Stage stage = (Stage) btnEnviar.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onEnviar(ActionEvent event) {
        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();
        if (correo.isEmpty()) {
            lblErrorCorreo.setText("Ingrese un correo electrónico válido.");
            lblErrorCorreo.setVisible(true);
            return;
        }

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT u.id_usuario FROM USUARIOS u WHERE u.correo = ?")) {
            statement.setString(1, correo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    lblErrorCorreo.setVisible(false);
                    paneExito.setVisible(true);
                } else {
                    lblErrorCorreo.setText("No existe ninguna cuenta vinculada a ese correo.");
                    lblErrorCorreo.setVisible(true);
                    paneExito.setVisible(false);
                }
            }
        } catch (SQLException ex) {
            lblErrorCorreo.setText("Error al consultar la base de datos: " + ex.getMessage());
            lblErrorCorreo.setVisible(true);
            paneExito.setVisible(false);
        }
    }
}
