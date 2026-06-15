package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalConfigurarPreguntaController {

    @FXML
    private ComboBox<String> cmbPregunta;
    @FXML
    private TextField txtRespuesta;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    @FXML
    private void initialize() {
        cmbPregunta.setItems(FXCollections.observableArrayList(
                "¿Cuál es el nombre de tu primera mascota?",
                "¿En qué ciudad naciste?",
                "¿Cuál es el apellido de soltera de tu madre?",
                "¿Cuál fue el nombre de tu escuela primaria?",
                "¿Cuál es tu película favorita?",
                "¿Cuál es el nombre de tu mejor amigo de la infancia?",
                "¿Cuál fue tu primer número de teléfono?",
                "¿Cuál es el modelo de tu primer automóvil?"
        ));
    }

    @FXML
    private void onGuardar() {
        String pregunta = cmbPregunta.getValue();
        String respuesta = txtRespuesta.getText() == null ? "" : txtRespuesta.getText().trim();

        // Validaciones
        if (pregunta == null || pregunta.isBlank()) {
            mostrarError("Selecciona una pregunta de seguridad.");
            return;
        }
        if (respuesta.isEmpty()) {
            mostrarError("Ingresa tu respuesta.");
            return;
        }
        if (respuesta.length() < 3) {
            mostrarError("La respuesta debe tener al menos 3 caracteres.");
            return;
        }

        int idUsuario = obtenerIdUsuario();
        if (idUsuario == -1) {
            mostrarError("Error de sesión. Cierra e inicia sesión de nuevo.");
            return;
        }

        String sql
                = "INSERT INTO PREGUNTAS_SEGURIDAD (id_usuario, pregunta, respuesta) "
                + "VALUES (?, ?, ?)";

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            stmt.setString(2, pregunta);
            stmt.setString(3, respuesta.toLowerCase());

            stmt.executeUpdate();

            ((Stage) btnGuardar.getScene().getWindow()).close();

        } catch (SQLException ex) {
            mostrarError("Error al guardar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private int obtenerIdUsuario() {
        String sql = "SELECT id_usuario FROM USUARIOS WHERE nombre_usuario = ?";
        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, SesionUsuario.getNombreUsuario());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_usuario");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}
