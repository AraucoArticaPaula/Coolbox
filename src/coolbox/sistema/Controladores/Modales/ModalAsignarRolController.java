package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Rol;
import coolbox.sistema.Modelos.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalAsignarRolController {

    @FXML
    private ComboBox<Usuario> cmbUsuario;

    @FXML
    private ComboBox<Rol> cmbRol;

    @FXML
    private Button btnGuardar;

    @FXML
    private void initialize() {
        loadUsuarios();
        loadRoles();
    }

    private void loadUsuarios() {
        String sql = "SELECT id_usuario, nombre_usuario, correo FROM USUARIOS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            var lista = FXCollections.<Usuario>observableArrayList();
            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setNombreUsuario(rs.getString("nombre_usuario"));
                usuario.setCorreo(rs.getString("correo"));
                lista.add(usuario);
            }
            cmbUsuario.setItems(lista);
        } catch (SQLException ignored) {
        }
    }

    private void loadRoles() {
        String sql = "SELECT id_rol, nombre_rol FROM ROLES";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            var lista = FXCollections.<Rol>observableArrayList();
            while (rs.next()) {
                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombreRol(rs.getString("nombre_rol"));
                lista.add(rol);
            }
            cmbRol.setItems(lista);
        } catch (SQLException ignored) {
        }
    }

    @FXML
    private void guardarAsignacionRol() {
        Usuario usuario = cmbUsuario.getValue();
        Rol rol = cmbRol.getValue();
        if (usuario == null || rol == null) {
            return;
        }
        String sql = "INSERT INTO USUARIOS_ROLES(id_usuario, id_rol) VALUES(?, ?)";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, usuario.getIdUsuario());
            statement.setInt(2, rol.getIdRol());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
        closeWindow();
    }

    @FXML
    private void cerrarModal() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
