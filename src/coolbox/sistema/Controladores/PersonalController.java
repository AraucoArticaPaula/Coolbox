package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Cumplimiento;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Horario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

public class PersonalController {

    @FXML private Button btnNuevoEmpleado;
    @FXML private Button btnAsignarCargo;
    @FXML private Button btnBajaEmpleado;
    
    @FXML private Label lblTiendaPrincipal; 

    @FXML private TableView<Empleado> tblEmpleados;
    @FXML private TableColumn<Empleado, Integer> colId;
    @FXML private TableColumn<Empleado, String> colNombres;
    @FXML private TableColumn<Empleado, String> colApellidos;
    @FXML private TableColumn<Empleado, String> colDNI;
    @FXML private TableColumn<Empleado, String> colCelular;
    @FXML private TableColumn<Empleado, String> colCorreo;
    @FXML private TableColumn<Empleado, String> colTipo;
    @FXML private TableColumn<Empleado, Integer> colTienda;

    @FXML private TableView<Horario> tblHorarios;
    @FXML private TableColumn<Horario, String> colHorarioEmpleado;
    @FXML private TableColumn<Horario, String> colHorarioDia;
    @FXML private TableColumn<Horario, String> colHorarioInicio;
    @FXML private TableColumn<Horario, String> colHorarioFin;

    @FXML private TableView<Cumplimiento> tblCumplimiento;
    @FXML private TableColumn<Cumplimiento, String> colCumpEmpleado;
    @FXML private TableColumn<Cumplimiento, String> colCumpHoras;
    @FXML private TableColumn<Cumplimiento, String> colCumpPeriodo;
    @FXML private TableColumn<Cumplimiento, String> colCumpEstado;

    @FXML
    private void initialize() {
        setupColumns();
        refreshAll();
        evaluarRestriccionesDeRol();
        buscarYMostrarNombreTiendaActiva();
    }

    private void buscarYMostrarNombreTiendaActiva() {
        int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
        String sql = "SELECT nombre_tienda FROM TIENDAS WHERE id_tienda = ?";
        String nombreTienda = "Sistemas Globales";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idTiendaSesion);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    nombreTienda = rs.getString("nombre_tienda");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (lblTiendaPrincipal != null) {
            boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
            if (esAdminGlobal) {
                lblTiendaPrincipal.setText("🌍 MODO: ADMINISTRADOR GLOBAL (TODAS LAS SEDES)");
                lblTiendaPrincipal.setStyle("-fx-background-color: #ECEFF1; -fx-text-fill: #37474F; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: bold;");
            } else {
                lblTiendaPrincipal.setText("🏪 SUCURSAL: " + nombreTienda.toUpperCase());
            }
        }
    }

    private void evaluarRestriccionesDeRol() {
        String rolActual = SesionUsuario.getRolUsuario();
        if (!"GERENTE".equalsIgnoreCase(rolActual) && !"ADMINISTRADOR".equalsIgnoreCase(rolActual)) {
            if (btnNuevoEmpleado != null) {
                btnNuevoEmpleado.setDisable(true);
                btnNuevoEmpleado.setText("🔒 Registro (Solo Gerente)");
            }
            if (btnBajaEmpleado != null) {
                btnBajaEmpleado.setDisable(true);
                btnBajaEmpleado.setText("🔒 Baja (Solo Gerente)");
            }
            if (btnAsignarCargo != null) {
                btnAsignarCargo.setDisable(true);
            }
        }
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCelular.setCellValueFactory(new PropertyValueFactory<>("celular"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTienda.setCellValueFactory(new PropertyValueFactory<>("idTienda"));

        colHorarioEmpleado.setCellValueFactory(new PropertyValueFactory<>("empleado"));
        colHorarioDia.setCellValueFactory(new PropertyValueFactory<>("dia"));
        colHorarioInicio.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        colHorarioFin.setCellValueFactory(new PropertyValueFactory<>("fin"));

        colCumpEmpleado.setCellValueFactory(new PropertyValueFactory<>("empleado"));
        colCumpHoras.setCellValueFactory(new PropertyValueFactory<>("horas"));
        colCumpPeriodo.setCellValueFactory(new PropertyValueFactory<>("periodo"));
        colCumpEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void refreshAll() {
        loadEmpleados();
        loadHorarios();
        loadCumplimiento();
    }

    @FXML
    private void abrirModalRegistrar() {
        openModal("/coolbox/sistema/Vistas/Modales/RegistrarEmpleado.fxml", "Registrar Empleado");
    }

    @FXML
    private void abrirModalAsignarCargo() {
        openModal("/coolbox/sistema/Vistas/Modales/AsignarCargo.fxml", "Asignar Cargo");
    }

    @FXML
    private void eliminarEmpleado() {
        Empleado empleado = tblEmpleados.getSelectionModel().getSelectedItem();
        if (empleado == null) return;

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM EMPLEADOS WHERE id_empleado = ?")) {
            statement.setInt(1, empleado.getId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
            empleado.setTipo("INACTIVO");
            empleado.setCorreo("BAJA");
            tblEmpleados.refresh();
            return;
        }
        refreshAll();
    }

    @FXML
    private void abrirModalRegistrarHorario() {
        openModal("/coolbox/sistema/Vistas/Modales/RegistrarHorario.fxml", "Registrar Horario");
    }

    @FXML
    private void abrirModalValidarCumplimiento() {
        openModal("/coolbox/sistema/Vistas/Modales/ValidarCumplimientoHoras.fxml", "Validar Cumplimiento");
    }

    private void loadEmpleados() {
        List<Empleado> empleados = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        int tiendaDeLaSesion = SesionUsuario.getIdTiendaUsuarioConectado();

        String sql = esAdminGlobal ? "SELECT id_empleado AS id, nombres, apellidos, DNI AS dni, celular, correo, tipo_empleado AS tipo, id_tienda FROM EMPLEADOS"
                                   : "SELECT id_empleado AS id, nombres, apellidos, DNI AS dni, celular, correo, tipo_empleado AS tipo, id_tienda FROM EMPLEADOS WHERE id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) {
                statement.setInt(1, tiendaDeLaSesion);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Empleado empleado = new Empleado();
                    empleado.setId(rs.getInt("id"));
                    empleado.setNombres(rs.getString("nombres"));
                    empleado.setApellidos(rs.getString("apellidos"));
                    empleado.setDni(rs.getString("dni"));
                    empleado.setCelular(rs.getString("celular"));
                    empleado.setCorreo(rs.getString("correo"));
                    empleado.setTipo(rs.getString("tipo"));
                    empleado.setIdTienda(rs.getInt("id_tienda"));
                    empleados.add(empleado);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        tblEmpleados.setItems(FXCollections.observableArrayList(empleados));
    }

    private void loadHorarios() {
        List<Horario> horarios = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        int tiendaDeLaSesion = SesionUsuario.getIdTiendaUsuarioConectado();

        String sql = "SELECT e.nombres + ' ' + e.apellidos AS empleado, h.dia_semana AS dia, h.hora_inicio AS inicio, h.hora_fin AS fin "
                + "FROM HORARIOS h INNER JOIN EMPLEADOS e ON h.id_empleado = e.id_empleado";
        if (!esAdminGlobal) sql += " WHERE e.id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) statement.setInt(1, tiendaDeLaSesion);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Horario horario = new Horario();
                    horario.setEmpleado(rs.getString("empleado"));
                    horario.setDia(rs.getString("dia"));
                    horario.setInicio(rs.getString("inicio"));
                    horario.setFin(rs.getString("fin"));
                    horarios.add(horario);
                }
            }
        } catch (SQLException ignored) {}
        tblHorarios.setItems(FXCollections.observableArrayList(horarios));
    }

    private void loadCumplimiento() {
        List<Cumplimiento> cumplimientos = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        int tiendaDeLaSesion = SesionUsuario.getIdTiendaUsuarioConectado();

        String sql = "SELECT e.nombres + ' ' + e.apellidos AS empleado, c.horas_semana AS horas, c.periodo, c.estado_validacion AS estado "
                + "FROM CUMPLIMIENTO_HORAS c INNER JOIN EMPLEADOS e ON c.id_empleado = e.id_empleado";
        if (!esAdminGlobal) sql += " WHERE e.id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) statement.setInt(1, tiendaDeLaSesion);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Cumplimiento cumplimiento = new Cumplimiento();
                    cumplimiento.setEmpleado(rs.getString("empleado"));
                    cumplimiento.setHoras(rs.getString("horas"));
                    cumplimiento.setPeriodo(rs.getString("periodo"));
                    cumplimiento.setEstado(rs.getString("estado"));
                    cumplimientos.add(cumplimiento);
                }
            }
        } catch (SQLException ignored) {}
        tblCumplimiento.setItems(FXCollections.observableArrayList(cumplimientos));
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
            refreshAll(); 
        } catch (Exception ignored) {}
    }

    @FXML private void irAPersonal() { openModule("/coolbox/sistema/Vistas/Personal.fxml", "Coolbox - Personal"); }
    
    // CORRECCIÓN CON CONTROL DE ACCESO INTEGRADO
    @FXML 
    private void irASeguridad() { 
        String rolActual = SesionUsuario.getRolUsuario();
        
        if ("ADMINISTRADOR".equalsIgnoreCase(rolActual)) {
            openModule("/coolbox/sistema/Vistas/Seguridad.fxml", "Coolbox - Seguridad"); 
        } else {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING, 
                "⛔ ACCESO RESTRINGIDO\n\nEl módulo de Seguridad y Control de Cuentas está reservado para los Administradores Globales.\n\nSu cargo actual [" + rolActual.toUpperCase() + "] no cuenta con las atribuciones requeridas.", 
                javafx.scene.control.ButtonType.OK
            );
            alert.setTitle("Restricción de Privilegios");
            alert.setHeaderText("Área de Acceso Protegido");
            
            Stage stageAlert = (Stage) alert.getDialogPane().getScene().getWindow();
            stageAlert.setResizable(false);
            
            alert.showAndWait();
        }
    }
    
    @FXML private void irAAlmacen() { openModule("/coolbox/sistema/Vistas/Almacen.fxml", "Coolbox - Almacén"); }
    @FXML private void irAOperaciones() { openModule("/coolbox/sistema/Vistas/Operaciones.fxml", "Coolbox - Operaciones"); }
    @FXML private void irAReportes() { openModule("/coolbox/sistema/Vistas/Reportes.fxml", "Coolbox - Reportes"); }
    @FXML private void cerrarSesion() { openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login"); }

    private void openModule(String resource, String title) {
        try {
            java.net.URL fxmlLocation = getClass().getResource(resource);
            if (fxmlLocation == null) {
                System.err.println("❌ No se encontró el archivo FXML en la ruta: " + resource);
                return;
            }
            
            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) tblEmpleados.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
            
        } catch (Exception e) {
            System.err.println("❌ Error al cambiar al módulo: " + title);
            e.printStackTrace();
        }
    }
}