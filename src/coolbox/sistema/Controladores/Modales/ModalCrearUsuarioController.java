package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Rol;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalCrearUsuarioController {

    @FXML private ComboBox<Empleado> cmbEmpleado;
    @FXML private TextField txtNombreUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtCorreo;
    @FXML private ComboBox<Rol> cmbRol;
    @FXML private Button btnGuardar;

    @FXML
    private void initialize() {
        configurarConvertidoresVisuales();
        loadEmpleadosSinUsuario();
        loadRoles();
    }

    private void configurarConvertidoresVisuales() {
        cmbEmpleado.setConverter(new StringConverter<Empleado>() {
            @Override
            public String toString(Empleado e) {
                return e == null ? "" : e.getNombres() + " " + e.getApellidos();
            }
            @Override
            public Empleado fromString(String string) { return null; }
        });

        cmbRol.setConverter(new StringConverter<Rol>() {
            @Override
            public String toString(Rol r) {
                return r == null ? "" : r.getNombreRol().toUpperCase();
            }
            @Override
            public Rol fromString(String string) { return null; }
        });
    }

    private void loadEmpleadosSinUsuario() {
        int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());

        // Seguridad por sedes implementada dinámicamente según las restricciones del negocio
        String sql = esAdminGlobal 
            ? "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_empleado NOT IN (SELECT id_empleado FROM USUARIOS)"
            : "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_tienda = ? AND id_empleado NOT IN (SELECT id_empleado FROM USUARIOS)";
        
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            if (!esAdminGlobal) {
                statement.setInt(1, idTiendaSesion);
            }
            
            try (ResultSet rs = statement.executeQuery()) {
                var lista = FXCollections.<Empleado>observableArrayList();
                while (rs.next()) {
                    Empleado empleado = new Empleado();
                    empleado.setId(rs.getInt("id_empleado")); 
                    empleado.setNombres(rs.getString("nombres"));
                    empleado.setApellidos(rs.getString("apellidos"));
                    lista.add(empleado);
                }
                cmbEmpleado.setItems(lista);
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarUsuario() {
        Empleado empleado = cmbEmpleado.getValue();
        Rol rol = cmbRol.getValue();
        String usuario = txtNombreUsuario.getText() == null ? "" : txtNombreUsuario.getText().trim();
        String contrasena = txtContrasena.getText() == null ? "" : txtContrasena.getText().trim();
        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();

        if (empleado == null || usuario.isEmpty() || contrasena.isEmpty() || correo.isEmpty() || rol == null) {
            mostrarAlerta("Campos vacíos", "Todos los campos son estrictamente obligatorios.", Alert.AlertType.WARNING);
            return;
        }

        String insertUsuario = "INSERT INTO USUARIOS(id_empleado, nombre_usuario, contrasena, correo, estado) VALUES(?, ?, ?, ?, 'activo')";
        Connection connection = null;
        
        try {
            connection = ConexionDB.getConnection();
            connection.setAutoCommit(false); // Transacción segura abierta

            try (PreparedStatement statement = connection.prepareStatement(insertUsuario, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, empleado.getId());
                statement.setString(2, usuario);
                statement.setString(3, contrasena);
                statement.setString(4, correo);
                statement.executeUpdate();
                
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int idUsuario = generatedKeys.getInt(1);
                        assignRole(idUsuario, rol.getIdRol(), connection);
                    }
                }
            }
            
            connection.commit();
            mostrarAlerta("Éxito", "Usuario asignado correctamente.", Alert.AlertType.INFORMATION);
            closeWindow();
            
        } catch (SQLException e) {
            if (connection != null) {
                try { 
                    connection.rollback(); // Control de contención ante errores en lote
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                }
            }
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo insertar el usuario: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (connection != null) {
                try { 
                    connection.setAutoCommit(true); 
                } catch (SQLException e) { 
                    e.printStackTrace(); 
                }
            }
        }
    }

    private void assignRole(int idUsuario, int idRol, Connection connection) throws SQLException {
        String insert = "INSERT INTO USUARIOS_ROLES(id_usuario, id_rol) VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idRol);
            statement.executeUpdate();
        }
    }

    @FXML
    private void cerrarModal() {
        closeWindow();
    }

    private void closeWindow() {
        if (btnGuardar != null && btnGuardar.getScene() != null) {
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}