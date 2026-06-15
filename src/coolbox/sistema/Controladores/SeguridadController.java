package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.RolePermission;
import coolbox.sistema.Modelos.Rol;
import coolbox.sistema.Modelos.UserRole;
import coolbox.sistema.Modelos.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeguridadController {

    @FXML
    private Label lblNombreUsuarioSidebar;

    @FXML
    private TableView<Usuario> tblUsuarios;

    @FXML
    private TableColumn<Usuario, String> colNombreEmpleado;

    @FXML
    private TableColumn<Usuario, String> colUsername;

    @FXML
    private TableColumn<Usuario, String> colCorreoUsuario;

    @FXML
    private TableColumn<Usuario, String> colEstadoUsuario;

    @FXML
    private TableView<UserRole> tblUsuariosRoles;

    @FXML
    private TableColumn<UserRole, String> colURUsuario;

    @FXML
    private TableColumn<UserRole, String> colURRol;

    @FXML
    private TableView<RolePermission> tblRolesPermisos;

    @FXML
    private TableColumn<RolePermission, String> colRPRol;

    @FXML
    private TableColumn<RolePermission, String> colRPPermiso;

    @FXML
    private void initialize() {
        mostrarNombreEnSidebar();

        setupColumns();
        refreshAll();
    }

    private void setupColumns() {
        colNombreEmpleado.setCellValueFactory(new PropertyValueFactory<>("nombreEmpleado"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colCorreoUsuario.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colEstadoUsuario.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colURUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colURRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        colRPRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colRPPermiso.setCellValueFactory(new PropertyValueFactory<>("permiso"));
    }

    private void refreshAll() {
        loadUsuarios();
        loadUsuarioRoles();
        loadRolesPermisos();
    }

    @FXML
    private void abrirModalCrearUsuario() {
        openModal("/coolbox/sistema/Vistas/Modales/CrearCuentaUsuario.fxml", "Crear usuario");
        refreshAll();
    }

    @FXML
    private void abrirModalAsignarRol() {
        openModal("/coolbox/sistema/Vistas/Modales/AsignarRolAdicional.fxml", "Asignar rol adicional");
        refreshAll();
    }

    private void loadUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.id_empleado, (e.nombres + ' ' + e.apellidos) AS nombre_completo, "
                + "u.nombre_usuario, u.correo, u.estado "
                + "FROM USUARIOS u "
                + "INNER JOIN EMPLEADOS e ON u.id_empleado = e.id_empleado";

        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Usuario usuario = new Usuario();

                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setIdEmpleado(rs.getInt("id_empleado"));
                usuario.setNombreEmpleado(rs.getString("nombre_completo"));
                usuario.setNombreUsuario(rs.getString("nombre_usuario"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setEstado(rs.getString("estado"));
                usuarios.add(usuario);
            }
        } catch (SQLException ignored) {
        }
        tblUsuarios.setItems(FXCollections.observableArrayList(usuarios));
    }

    private void loadUsuarioRoles() {
        List<UserRole> usuarioRoles = new ArrayList<>();
        String sql = "SELECT u.nombre_usuario AS usuario, r.nombre_rol AS rol "
                + "FROM USUARIOS_ROLES ur "
                + "LEFT JOIN USUARIOS u ON ur.id_usuario = u.id_usuario "
                + "LEFT JOIN ROLES r ON ur.id_rol = r.id_rol";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                UserRole usuarioRol = new UserRole();
                usuarioRol.setUsuario(rs.getString("usuario"));
                usuarioRol.setRol(rs.getString("rol"));
                usuarioRoles.add(usuarioRol);
            }
        } catch (SQLException ignored) {
        }
        tblUsuariosRoles.setItems(FXCollections.observableArrayList(usuarioRoles));
    }

    private void loadRolesPermisos() {
        List<RolePermission> permisos = new ArrayList<>();
        String sql = "SELECT r.nombre_rol AS rol, p.nombre_permiso AS permiso "
                + "FROM ROLES_PERMISOS rp "
                + "LEFT JOIN ROLES r ON rp.id_rol = r.id_rol "
                + "LEFT JOIN PERMISOS p ON rp.id_permiso = p.id_permiso";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                RolePermission permiso = new RolePermission();
                permiso.setRol(rs.getString("rol"));
                permiso.setPermiso(rs.getString("permiso"));
                permisos.add(permiso);
            }
        } catch (SQLException ignored) {
        }
        tblRolesPermisos.setItems(FXCollections.observableArrayList(permisos));
    }

    private void openModal(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void irPersonal() {
        openModule("/coolbox/sistema/Vistas/Personal.fxml", "Coolbox - Personal");
    }

    @FXML
    private void irOperaciones() {
        openModule("/coolbox/sistema/Vistas/Operaciones.fxml", "Coolbox - Operaciones");
    }

    @FXML
    private void irAlmacen() {
        openModule("/coolbox/sistema/Vistas/Almacen.fxml", "Coolbox - Almacén");
    }

    @FXML
    private void irReportes() {
        openModule("/coolbox/sistema/Vistas/Reportes.fxml", "Coolbox - Reportes");
    }

    @FXML
    private void cerrarSesion() {
        openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login");
    }

    private void openModule(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = (Stage) tblUsuarios.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void abrirPerfil() {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                    getClass().getResource("/coolbox/sistema/Vistas/Modales/PerfilUsuario.fxml"));
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Mi perfil");
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarNombreEnSidebar() {
        if (lblNombreUsuarioSidebar != null) {
            String nombreReal = coolbox.sistema.Controladores.SesionUsuario.getNombresCompletos();
            String usuarioRed = coolbox.sistema.Controladores.SesionUsuario.getNombreUsuario();

            System.out.println("DEBUG - Nombre Real: " + nombreReal + " | Username: " + usuarioRed);

            if (nombreReal != null && !nombreReal.trim().isEmpty() && !nombreReal.equalsIgnoreCase("null null")) {
                lblNombreUsuarioSidebar.setText(nombreReal);
            } else if (usuarioRed != null && !usuarioRed.trim().isEmpty()) {
                lblNombreUsuarioSidebar.setText("Usuario: " + usuarioRed);
            } else {
                lblNombreUsuarioSidebar.setText("Cargando...");
            }
        }
    }

}
