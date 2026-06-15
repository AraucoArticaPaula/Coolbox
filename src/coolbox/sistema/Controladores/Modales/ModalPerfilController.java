package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalPerfilController {

    @FXML
    private TextField txtNombres;
    @FXML
    private TextField txtApellidos;
    @FXML
    private TextField txtDni;
    @FXML
    private TextField txtNombreUsuario;
    @FXML
    private TextField txtCelular;
    @FXML
    private TextField txtCorreo;
    @FXML
    private PasswordField txtNuevaClave;
    @FXML
    private PasswordField txtConfirmarClave;
    @FXML
    private Label lblMensaje;

    private int idEmpleado;
    private int idUsuario;

    @FXML
    private void initialize() {
        idEmpleado = SesionUsuario.getIdEmpleado();
        cargarDatos();
    }

    private void cargarDatos() {
        String sql = "SELECT e.nombres, e.apellidos, e.DNI, e.celular, e.correo, "
                + "u.nombre_usuario, u.id_usuario "
                + "FROM EMPLEADOS e "
                + "INNER JOIN USUARIOS u ON u.id_empleado = e.id_empleado "
                + "WHERE e.id_empleado = ?";

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idEmpleado);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    txtNombres.setText(rs.getString("nombres"));
                    txtApellidos.setText(rs.getString("apellidos"));
                    txtDni.setText(rs.getString("DNI"));
                    txtNombreUsuario.setText(rs.getString("nombre_usuario"));
                    txtCelular.setText(rs.getString("celular") != null ? rs.getString("celular") : "");
                    txtCorreo.setText(rs.getString("correo") != null ? rs.getString("correo") : "");
                    idUsuario = rs.getInt("id_usuario");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar datos.");
        }
    }

    @FXML
    private void guardar() {
        String celular = txtCelular.getText().trim();
        String correo = txtCorreo.getText().trim();
        String nuevaClave = txtNuevaClave.getText();
        String confirmarClave = txtConfirmarClave.getText();

        if (correo.isEmpty()) {
            lblMensaje.setText("El correo no puede estar vacío.");
            return;
        }

        if (!nuevaClave.isEmpty()) {
            if (!nuevaClave.equals(confirmarClave)) {
                lblMensaje.setText("Las contraseñas no coinciden.");
                return;
            }
            if (nuevaClave.length() < 6) {
                lblMensaje.setText("La contraseña debe tener al menos 6 caracteres.");
                return;
            }
        }

        try (Connection con = ConexionDB.getConnection()) {

            try (PreparedStatement stmt = con.prepareStatement(
                    "UPDATE EMPLEADOS SET celular = ?, correo = ? WHERE id_empleado = ?")) {
                stmt.setString(1, celular);
                stmt.setString(2, correo);
                stmt.setInt(3, idEmpleado);
                stmt.executeUpdate();
            }

            if (!nuevaClave.isEmpty()) {
                try (PreparedStatement stmt = con.prepareStatement(
                        "UPDATE USUARIOS SET contrasena = ? WHERE id_usuario = ?")) {
                    stmt.setString(1, nuevaClave);
                    stmt.setInt(2, idUsuario);
                    stmt.executeUpdate();
                }
            }

            lblMensaje.setStyle("-fx-text-fill: #2E7D32;");
            lblMensaje.setText("✓ Cambios guardados.");
            txtNuevaClave.clear();
            txtConfirmarClave.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            lblMensaje.setStyle("-fx-text-fill: #CC0000;");
            lblMensaje.setText("Error al guardar los cambios.");
        }
    }

    @FXML
    private void cancelar() {
        ((Stage) txtNombres.getScene().getWindow()).close();
    }
}
