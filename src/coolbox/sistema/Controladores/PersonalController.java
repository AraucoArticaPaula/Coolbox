package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Cumplimiento;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Horario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class PersonalController {

    @FXML private Button btnNuevoEmpleado;
    @FXML private Button btnBajaEmpleado;
    @FXML private Button btnDeshabilitarEmpleado;
    @FXML private Button btnCambiarTiendaCargo; 
    
    @FXML private Label lblNombreUsuarioSidebar;
    @FXML private Label lblTiendaPrincipal; 
    @FXML private Label lblFechaActualHorario;

    @FXML private DatePicker dpHorarioInicio;
    @FXML private DatePicker dpHorarioFin;
    @FXML private ComboBox<String> cmbHorarioDia;

    @FXML private TableView<Empleado> tblEmpleados;
    @FXML private TableColumn<Empleado, String> colNombres;
    @FXML private TableColumn<Empleado, String> colApellidos;
    @FXML private TableColumn<Empleado, String> colDNI;
    @FXML private TableColumn<Empleado, String> colCelular;
    @FXML private TableColumn<Empleado, String> colCorreo;
    @FXML private TableColumn<Empleado, String> colTipo;
    @FXML private TableColumn<Empleado, String> colNombreTienda;

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
        mostrarNombreEnSidebar(); 
        setupColumns();
        configurarFiltrosHorario();
        refreshAll();
        evaluarRestriccionesDeRol();
        buscarYMostrarNombreTiendaActiva();

        tblEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (btnDeshabilitarEmpleado == null) return;
            if (selected == null) {
                btnDeshabilitarEmpleado.setText("⏸ Deshabilitar");
                return;
            }
            int idSesion = SesionUsuario.getIdEmpleado();
            if (selected.getId() == idSesion) {
                btnDeshabilitarEmpleado.setDisable(true);
                btnDeshabilitarEmpleado.setText("⏸ Deshabilitar");
                if (btnBajaEmpleado != null) btnBajaEmpleado.setDisable(true);
                return;
            }
            
            String rolBaja = SesionUsuario.getRolUsuario();
            boolean puedeGestionarBaja = "GERENTE".equalsIgnoreCase(rolBaja) || "ADMINISTRADOR".equalsIgnoreCase(rolBaja);
            if (btnBajaEmpleado != null) btnBajaEmpleado.setDisable(!puedeGestionarBaja);
            
            String rol = SesionUsuario.getRolUsuario();
            boolean puedeGestionar = "GERENTE".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol);
            btnDeshabilitarEmpleado.setDisable(!puedeGestionar);

            String estado = selected.getEstado();
            boolean deshabilitado = "DESHABILITADO".equalsIgnoreCase(estado);
            btnDeshabilitarEmpleado.setText(deshabilitado ? "▶ Habilitar" : "⏸ Deshabilitar");
            btnDeshabilitarEmpleado.setStyle(deshabilitado
                ? "-fx-background-color: #28A745; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;"
                : "-fx-background-color: #FFC107; -fx-text-fill: #212121; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        });
    }

    private void configurarFiltrosHorario() {
        LocalDate hoy = LocalDate.now();
        if (lblFechaActualHorario != null) {
            lblFechaActualHorario.setText("📅 Hoy: " + hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        if (dpHorarioInicio != null && dpHorarioFin != null) {
            dpHorarioInicio.setValue(hoy);
            dpHorarioFin.setValue(hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            actualizarComboDias(hoy);

            dpHorarioInicio.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    dpHorarioFin.setValue(newV.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
                    actualizarComboDias(newV);
                }
            });
        }
    }

    private void actualizarComboDias(LocalDate fechaSeleccionada) {
        if (cmbHorarioDia == null || fechaSeleccionada == null) return;
        cmbHorarioDia.getItems().clear();
        cmbHorarioDia.getItems().add("Todos");
        
        int diaActual = fechaSeleccionada.getDayOfWeek().getValue();
        
        if (diaActual <= 1) cmbHorarioDia.getItems().add("Lunes");
        if (diaActual <= 2) cmbHorarioDia.getItems().add("Martes");
        if (diaActual <= 3) cmbHorarioDia.getItems().add("Miercoles");
        if (diaActual <= 4) cmbHorarioDia.getItems().add("Jueves");
        if (diaActual <= 5) cmbHorarioDia.getItems().add("Viernes");
        if (diaActual <= 6) cmbHorarioDia.getItems().add("Sabado");
        if (diaActual <= 7) cmbHorarioDia.getItems().add("Domingo");
        
        cmbHorarioDia.getSelectionModel().select("Todos");
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
            if (btnDeshabilitarEmpleado != null) {
                btnDeshabilitarEmpleado.setDisable(true);
                btnDeshabilitarEmpleado.setText("🔒 Deshabilitar (Solo Gerente)");
            }
            if (btnCambiarTiendaCargo != null) {
                btnCambiarTiendaCargo.setDisable(true);
            }
        }
    }

    private void setupColumns() {
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCelular.setCellValueFactory(new PropertyValueFactory<>("celular"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colNombreTienda.setCellValueFactory(new PropertyValueFactory<>("nombreTienda"));

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

    @FXML private void abrirModalRegistrar() { openModal("/coolbox/sistema/Vistas/Modales/RegistrarEmpleado.fxml", "Registrar Empleado"); }
    @FXML private void abrirModalRegistrarHorario() { openModal("/coolbox/sistema/Vistas/Modales/RegistrarHorario.fxml", "Registrar Horario"); }
    @FXML private void abrirModalValidarCumplimiento() { openModal("/coolbox/sistema/Vistas/Modales/ValidarCumplimientoHoras.fxml", "Validar Cumplimiento"); }

    @FXML
    private void abrirModalCambiarTiendaCargo() {
        Empleado empleadoSeleccionado = tblEmpleados.getSelectionModel().getSelectedItem();
        
        if (empleadoSeleccionado == null) {
            mostrarAlerta("Por favor, selecciona un empleado de la lista primero.");
            return;
        }

        try {
            URL url = getClass().getResource("/coolbox/sistema/Vistas/Modales/CambiarTiendaCargo.fxml");
            if (url == null) {
                mostrarAlerta("No se encuentra CambiarTiendaCargo.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            coolbox.sistema.Controladores.Modales.ModalCambiarTiendaCargoController controller = loader.getController();
            controller.initData(empleadoSeleccionado);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Cambiar Tienda y Cargo");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait(); 
            
            refreshAll(); 
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al abrir la ventana de cambio de tienda y cargo.");
        }
    }

    @FXML
    private void eliminarEmpleado() {
        Empleado empleado = tblEmpleados.getSelectionModel().getSelectedItem();
        if (empleado == null) { mostrarAlerta("Selecciona un empleado primero."); return; }
        if (empleado.getId() == SesionUsuario.getIdEmpleado()) { mostrarAlerta("No puedes darte de baja a ti mismo."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Dar de Baja");
        confirm.setHeaderText("¿Dar de baja a " + empleado.getNombres() + " " + empleado.getApellidos() + "?");
        confirm.setContentText("El empleado quedará marcado como BAJA pero sus datos e historial se conservarán.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) cambiarEstadoEmpleado(empleado.getId(), "BAJA");
        });
    }

    @FXML
    private void deshabilitarEmpleado() {
        Empleado empleado = tblEmpleados.getSelectionModel().getSelectedItem();
        if (empleado == null) { mostrarAlerta("Selecciona un empleado primero."); return; }
        if (empleado.getId() == SesionUsuario.getIdEmpleado()) { mostrarAlerta("No puedes deshabilitarte a ti mismo."); return; }

        String estadoActual = empleado.getEstado();
        boolean estaActivo = estadoActual == null || "ACTIVO".equalsIgnoreCase(estadoActual);
        String nuevoEstado = estaActivo ? "DESHABILITADO" : "ACTIVO";
        String accion = estaActivo ? "deshabilitar" : "rehabilitar";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(estaActivo ? "Deshabilitar Empleado" : "Rehabilitar Empleado");
        confirm.setHeaderText("¿Deseas " + accion + " a " + empleado.getNombres() + " " + empleado.getApellidos() + "?");
        confirm.setContentText(estaActivo ? "El empleado no aparecerá en operaciones activas." : "El empleado volverá a estar activo.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) cambiarEstadoEmpleado(empleado.getId(), nuevoEstado);
        });
    }

    private void cambiarEstadoEmpleado(int idEmpleado, String nuevoEstado) {
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement stmt = connection.prepareStatement("UPDATE EMPLEADOS SET estado = ? WHERE id_empleado = ?")) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idEmpleado);
            stmt.executeUpdate();
            refreshAll();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error al actualizar el estado del empleado.");
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void loadEmpleados() {
        List<Empleado> empleados = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        int tiendaDeLaSesion = SesionUsuario.getIdTiendaUsuarioConectado();

        String sql = esAdminGlobal
                ? "SELECT e.id_empleado AS id, e.nombres, e.apellidos, e.DNI AS dni, e.celular, e.correo, e.tipo_empleado AS tipo, e.id_tienda, t.nombre_tienda, e.estado " +
                  "FROM EMPLEADOS e INNER JOIN TIENDAS t ON e.id_tienda = t.id_tienda " +
                  "WHERE ISNULL(e.estado, 'ACTIVO') <> 'BAJA'"
                : "SELECT e.id_empleado AS id, e.nombres, e.apellidos, e.DNI AS dni, e.celular, e.correo, e.tipo_empleado AS tipo, e.id_tienda, t.nombre_tienda, e.estado " +
                  "FROM EMPLEADOS e INNER JOIN TIENDAS t ON e.id_tienda = t.id_tienda " +
                  "WHERE e.id_tienda = ? AND ISNULL(e.estado, 'ACTIVO') <> 'BAJA'";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) statement.setInt(1, tiendaDeLaSesion);
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
                    empleado.setNombreTienda(rs.getString("nombre_tienda")); 
                    empleado.setEstado(rs.getString("estado"));
                    empleados.add(empleado);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        tblEmpleados.setItems(FXCollections.observableArrayList(empleados));
    }

    @FXML
    private void buscarHorarios() {
        loadHorarios();
    }

    private void loadHorarios() {
        if (tblHorarios == null) return;
        List<Horario> horarios = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        
        LocalDate ini = (dpHorarioInicio != null && dpHorarioInicio.getValue() != null) ? dpHorarioInicio.getValue() : LocalDate.now();
        String diaFiltro = (cmbHorarioDia != null && cmbHorarioDia.getValue() != null) ? cmbHorarioDia.getValue() : "Todos";

        LocalDate lunesSemana = ini.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingoSemana = ini.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        StringBuilder sql = new StringBuilder("SELECT e.nombres + ' ' + e.apellidos AS empleado, h.dia_semana AS dia, h.hora_inicio AS inicio, h.hora_fin AS fin "
                + "FROM HORARIOS h INNER JOIN EMPLEADOS e ON h.id_empleado = e.id_empleado "
                + "WHERE h.fecha_inicio = ? AND h.fecha_fin = ?");

        if (!esAdminGlobal) sql.append(" AND e.id_tienda = ?");

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            statement.setDate(paramIndex++, java.sql.Date.valueOf(lunesSemana));
            statement.setDate(paramIndex++, java.sql.Date.valueOf(domingoSemana));
            
            if (!esAdminGlobal) statement.setInt(paramIndex++, SesionUsuario.getIdTiendaUsuarioConectado());

            int diaSeleccionadoIndex = ini.getDayOfWeek().getValue();

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String diaBD = rs.getString("dia");
                    int diaBDIndex = mapDiaANumero(diaBD);

                    if (!"Todos".equals(diaFiltro) && !diaFiltro.equalsIgnoreCase(diaBD)) {
                        continue;
                    }
                    if (diaBDIndex < diaSeleccionadoIndex) {
                        continue;
                    }

                    Horario horario = new Horario();
                    horario.setEmpleado(rs.getString("empleado"));
                    horario.setDia(diaBD);
                    horario.setInicio(rs.getString("inicio"));
                    horario.setFin(rs.getString("fin"));
                    horarios.add(horario);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        tblHorarios.setItems(FXCollections.observableArrayList(horarios));
    }

    private int mapDiaANumero(String dia) {
        switch(dia.toUpperCase()) {
            case "LUNES": return 1;
            case "MARTES": return 2;
            case "MIERCOLES": return 3;
            case "JUEVES": return 4;
            case "VIERNES": return 5;
            case "SABADO": return 6;
            case "DOMINGO": return 7;
            default: return 0;
        }
    }

    private void loadCumplimiento() {
        if (tblCumplimiento == null) return;
        List<Cumplimiento> cumplimientos = new ArrayList<>();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());

        String sql = "SELECT e.nombres + ' ' + e.apellidos AS empleado, c.horas_semana AS horas, c.periodo, c.estado_validacion AS estado "
                + "FROM CUMPLIMIENTO_HORAS c INNER JOIN EMPLEADOS e ON c.id_empleado = e.id_empleado";
        if (!esAdminGlobal) sql += " WHERE e.id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) statement.setInt(1, SesionUsuario.getIdTiendaUsuarioConectado());
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
    @FXML private void irAAlmacen() { openModule("/coolbox/sistema/Vistas/Almacen.fxml", "Coolbox - Almacén"); }
    @FXML private void irAOperaciones() { openModule("/coolbox/sistema/Vistas/Operaciones.fxml", "Coolbox - Operaciones"); }
    @FXML private void irAReportes() { openModule("/coolbox/sistema/Vistas/Reportes.fxml", "Coolbox - Reportes"); }
    @FXML private void cerrarSesion() { openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login"); }

    @FXML 
    private void irASeguridad() { 
        String rolActual = SesionUsuario.getRolUsuario();
        if ("ADMINISTRADOR".equalsIgnoreCase(rolActual)) {
            openModule("/coolbox/sistema/Vistas/Seguridad.fxml", "Coolbox - Seguridad"); 
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "⛔ NO AUTORIZADO.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void openModule(String resource, String title) {
        try {
            java.net.URL fxmlLocation = getClass().getResource(resource);
            if (fxmlLocation == null) return;
            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) tblEmpleados.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void abrirPerfil() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/coolbox/sistema/Vistas/Modales/PerfilUsuario.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Mi Perfil");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void mostrarNombreEnSidebar() {
        if (lblNombreUsuarioSidebar != null) {
            String nombreReal = SesionUsuario.getNombresCompletos();
            String usuarioRed = SesionUsuario.getNombreUsuario();
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