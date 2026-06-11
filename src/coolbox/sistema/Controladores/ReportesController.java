package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Comision;
import coolbox.sistema.Modelos.ComisionResumen;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportesController {

    @FXML private ComboBox<String> cmbTipoFiltro; 
    @FXML private ComboBox<Empleado> cmbEmpleado;
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    
    @FXML private Label lblNombreUsuarioSidebar;
    @FXML private Label lblMontoVsPresupuesto;
    @FXML private Label lblEstadoPresupuesto; 
    @FXML private Label lblTituloPresupuesto; 
    @FXML private Label lblPorcentajeGarantias;
    @FXML private Label lblNumTransacciones;
    @FXML private Label lblLlenaLaBolsa;
    @FXML private Label lblTiendaPrincipal;

    @FXML private TableView<Comision> tblComisiones;
    @FXML private TableColumn<Comision, String> colTipoComision;
    @FXML private TableColumn<Comision, Double> colMontoComision;
    @FXML private TableColumn<Comision, String> colFechaComision;
    @FXML private TableColumn<Comision, String> colIdVentaComision; // Cambiado a String para alojar el Nº Boleta
    @FXML private TableColumn<Comision, Double> colMontoVenta;

    @FXML private TableView<ComisionResumen> tblAcumuladoComisiones;
    @FXML private TableColumn<ComisionResumen, String> colAcumuladoTipo;
    @FXML private TableColumn<ComisionResumen, Double> colAcumuladoTotal;

    private int idTiendaUsuarioActivo = -1;
    private String nombreTiendaActiva = "";

    @FXML
    private void initialize() {
        setupColumns();
        identificarTiendaSesionNativa(); 
        loadFiltrosYCombos();
        
        dpFechaInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFechaFin.setValue(LocalDate.now());
    }

    private void setupColumns() {
        colTipoComision.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMontoComision.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colFechaComision.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colIdVentaComision.setCellValueFactory(new PropertyValueFactory<>("numeroBoleta")); 
        colMontoVenta.setCellValueFactory(new PropertyValueFactory<>("montoVenta"));

        colAcumuladoTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colAcumuladoTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
    }

    private void identificarTiendaSesionNativa() {
        idTiendaUsuarioActivo = SesionUsuario.getIdTiendaUsuarioConectado();
        
        if (idTiendaUsuarioActivo > 0) {
            String sql = "SELECT nombre_tienda FROM TIENDAS WHERE id_tienda = ?";
            try (Connection con = ConexionDB.getConnection();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, idTiendaUsuarioActivo);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        nombreTiendaActiva = rs.getString("nombre_tienda");
                        lblTiendaPrincipal.setText("🏪 SUCURSAL: " + nombreTiendaActiva.toUpperCase());
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            lblTiendaPrincipal.setText("🏪 SUCURSAL: TIENDA CENTRAL");
        }
    }

    private void loadFiltrosYCombos() {
        cmbTipoFiltro.setItems(FXCollections.observableArrayList("Tienda", "Empleado"));
        cmbTipoFiltro.setValue("Tienda"); 

        loadEmpleadosSede(idTiendaUsuarioActivo);

        cmbTipoFiltro.valueProperty().addListener((observable, oldValue, nuevoEnfoque) -> {
            if ("Tienda".equals(nuevoEnfoque)) {
                cmbEmpleado.setValue(null);
                cmbEmpleado.setDisable(true);
                lblTituloPresupuesto.setText("MONTO VS PPTO");
            } else {
                cmbEmpleado.setDisable(false);
                lblTituloPresupuesto.setText("TOTAL VENTAS CAJERO");
            }
        });
        
        cmbEmpleado.setDisable(true); 
    }

    private void loadEmpleadosSede(int idTienda) {
        List<Empleado> empleados = new ArrayList<>();
        String sql = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_tienda = ?";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idTienda);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Empleado empleado = new Empleado();
                    empleado.setId(rs.getInt("id_empleado"));
                    empleado.setNombres(rs.getString("nombres"));
                    empleado.setApellidos(rs.getString("apellidos"));
                    empleados.add(empleado);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        cmbEmpleado.setItems(FXCollections.observableArrayList(empleados));
    }

    @FXML
    private void consultarReportes() {
        LocalDate inicio = dpFechaInicio.getValue();
        LocalDate fin = dpFechaFin.getValue();
        if (inicio == null || fin == null) return;

        String enfoque = cmbTipoFiltro.getValue();
        
        if ("Empleado".equals(enfoque) && cmbEmpleado.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "⚠️ Seleccione un empleado válido para efectuar el análisis.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        int empleadoId = (cmbEmpleado.getValue() != null) ? cmbEmpleado.getValue().getId() : -1;
        procesarAnaliticaCompleta(inicio, fin, enfoque, empleadoId, idTiendaUsuarioActivo);
    }

    private void procesarAnaliticaCompleta(LocalDate inicio, LocalDate fin, String enfoque, int empleadoId, int tiendaId) {
        List<Comision> listaComisiones = new ArrayList<>();
        Map<String, Double> acumuladoMap = new HashMap<>();
        
        acumuladoMap.put("Garantía Clásica (2.5%)", 0.0);
        acumuladoMap.put("Garantía Plus (5%)", 0.0);
        acumuladoMap.put("Garantía Premium (10%)", 0.0);
        acumuladoMap.put("Bono Llena la Bolsa (5%)", 0.0);

        double totalVentaSoles = 0;
        int totalTransacciones = 0;
        int totalItemsVendidos = 0;
        int totalGarantiasVendidas = 0;

        StringBuilder sql = new StringBuilder(
            "SELECT v.id_venta, v.monto, CONVERT(varchar, v.fecha, 120) AS f_venta, " +
            "v.productos_por_venta, v.numero_boleta " +
            "FROM VENTAS v WHERE CAST(v.fecha AS DATE) BETWEEN ? AND ? AND v.id_tienda = ?"
        );
        if ("Empleado".equals(enfoque) && empleadoId > 0) {
            sql.append(" AND v.id_empleado = ").append(empleadoId);
        }
        sql.append(" ORDER BY v.id_venta DESC");

        try (Connection con = ConexionDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            stmt.setDate(1, Date.valueOf(inicio));
            stmt.setDate(2, Date.valueOf(fin));
            stmt.setInt(3, tiendaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    totalTransacciones++;
                    int idVenta = rs.getInt("id_venta");
                    double montoVenta = rs.getDouble("monto");
                    int cantItems = rs.getInt("productos_por_venta");
                    String fechaVenta = rs.getString("f_venta");
                    String nroBoleta = rs.getString("numero_boleta");

                    totalVentaSoles += montoVenta;
                    totalItemsVendidos += cantItems;

                    String sqlDetalle = "SELECT nombre_producto, precio_unitario FROM DETALLE_VENTA WHERE id_venta = ?";
                    try (PreparedStatement stmtDet = con.prepareStatement(sqlDetalle)) {
                        stmtDet.setInt(1, idVenta);
                        try (ResultSet rsDet = stmtDet.executeQuery()) {
                            while (rsDet.next()) {
                                String producto = rsDet.getString("nombre_producto");
                                double precioProd = rsDet.getDouble("precio_unitario");

                                if (producto != null && producto.toUpperCase().contains("GARANTIA")) {
                                    totalGarantiasVendidas++;
                                    String tipoCom = "Garantía Clásica (2.5%)";
                                    double porcentaje = 0.025;

                                    if (producto.toUpperCase().contains("PLUS")) {
                                        tipoCom = "Garantía Plus (5%)";
                                        porcentaje = 0.05;
                                    } else if (producto.toUpperCase().contains("PREMIUM")) {
                                        tipoCom = "Garantía Premium (10%)";
                                        porcentaje = 0.10;
                                    }

                                    double comisionCalculada = Math.round((precioProd * porcentaje) * 100.0) / 100.0;
                                    
                                    Comision c = new Comision();
                                    c.setTipo(tipoCom);
                                    c.setMonto(comisionCalculada);
                                    c.setFecha(fechaVenta);
                                    c.setNumeroBoleta(nroBoleta); 
                                    c.setMontoVenta(montoVenta);
                                    listaComisiones.add(c);

                                    acumuladoMap.put(tipoCom, acumuladoMap.getOrDefault(tipoCom, 0.0) + comisionCalculada);
                                }
                            }
                        }
                    }
                }
            }

            // 🌟 CÁLCULO DE LLB REFORMULADO: Ítems Totales / Transacciones Totales
            double llbIndicadorFinal = totalTransacciones > 0 ? ((double) totalItemsVendidos / totalTransacciones) : 0.00;
            lblLlenaLaBolsa.setText(String.format("%.2f", llbIndicadorFinal));

            // Inyección del Bono Llena la Bolsa (5%) si el ratio final supera los 2.0 artículos por boleta
            if (llbIndicadorFinal > 2.00 && totalVentaSoles > 0 && "Empleado".equals(enfoque)) {
                double bonoLLB = Math.round((totalVentaSoles * 0.05) * 100.0) / 100.0;
                acumuladoMap.put("Bono Llena la Bolsa (5%)", bonoLLB);
                
                Comision cBono = new Comision();
                cBono.setTipo("Bono Llena la Bolsa (5%)");
                cBono.setMonto(bonoLLB);
                cBono.setFecha(LocalDate.now().toString());
                cBono.setNumeroBoleta("BONO PERIODO"); 
                cBono.setMontoVenta(totalVentaSoles);
                listaComisiones.add(0, cBono);
            }

        } catch (SQLException e) { e.printStackTrace(); }

        tblComisiones.setItems(FXCollections.observableArrayList(listaComisiones));
        
        List<ComisionResumen> listaResumen = new ArrayList<>();
        acumuladoMap.forEach((tipo, total) -> {
            double totalRedondeado = Math.round(total * 100.0) / 100.0;
            listaResumen.add(new ComisionResumen(tipo, totalRedondeado));
        });
        tblAcumuladoComisiones.setItems(FXCollections.observableArrayList(listaResumen));

        lblNumTransacciones.setText(String.valueOf(totalTransacciones));

        double tasaGarantias = totalItemsVendidos > 0 ? ((double) totalGarantiasVendidas / totalItemsVendidos) * 100 : 0;
        lblPorcentajeGarantias.setText(String.format("%.2f %%", tasaGarantias));

        if ("Tienda".equals(enfoque)) {
            calcularPresupuestoTiendaCard(tiendaId, inicio, fin, totalVentaSoles);
        } else {
            lblMontoVsPresupuesto.setText(String.format("VENTAS: S/ %.2f", totalVentaSoles));
            lblEstadoPresupuesto.setText(""); 
        }
    }

    private void calcularPresupuestoTiendaCard(int idTienda, LocalDate inicio, LocalDate fin, double ventasRealizadas) {
        double presupuestoMensual = 0.0;
        String sql = "SELECT ISNULL(presupuesto_tienda, 0) AS meta FROM TIENDAS WHERE id_tienda = ?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idTienda);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) presupuestoMensual = rs.getDouble("meta");
            }
        } catch (SQLException e) { e.printStackTrace(); }

        long diasFiltro = ChronoUnit.DAYS.between(inicio, fin) + 1;
        int diasDelMes = inicio.lengthOfMonth();
        double cuotaDiaria = presupuestoMensual / diasDelMes;
        double presupuestoProrrateado = cuotaDiaria * diasFiltro;

        double saldoFaltante = presupuestoProrrateado - ventasRealizadas;

        lblMontoVsPresupuesto.setText(String.format("VENTAS: S/ %.2f", ventasRealizadas));

        if (saldoFaltante <= 0) {
            lblEstadoPresupuesto.setText(String.format("META: S/ %.2f\nCOMPLETADO 🎉", presupuestoProrrateado));
            lblEstadoPresupuesto.setStyle("-fx-text-fill: #1CC88A; -fx-font-weight: bold; -fx-font-size: 11px;"); 
        } else {
            lblEstadoPresupuesto.setText(String.format("META: S/ %.2f\nFALTA: S/ %.2f", presupuestoProrrateado, saldoFaltante));
            lblEstadoPresupuesto.setStyle("-fx-text-fill: #E74A3B; -fx-font-weight: bold; -fx-font-size: 11px;"); 
        }
    }

    @FXML private void irPersonal() { openModule("/coolbox/sistema/Vistas/Personal.fxml", "Coolbox - Personal"); }
    @FXML private void irAlmacen() { openModule("/coolbox/sistema/Vistas/Almacen.fxml", "Coolbox - Almacén"); }
    @FXML private void irOperaciones() { openModule("/coolbox/sistema/Vistas/Operaciones.fxml", "Coolbox - Operaciones"); }
    @FXML private void cerrarSesion() { openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login"); }

    @FXML 
    private void irSeguridad() { 
        String rolActual = "INVITADO";
        try {
            if (SesionUsuario.getRolUsuario() != null) {
                rolActual = SesionUsuario.getRolUsuario();
            }
        } catch (Throwable t) { System.err.println("No se pudo obtener el rol."); }

        if ("ADMINISTRADOR".equalsIgnoreCase(rolActual)) {
            openModule("/coolbox/sistema/Vistas/Seguridad.fxml", "Coolbox - Seguridad"); 
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "⛔ ACCESO RESTRINGIDO\n\nSolo Administradores tienen acceso a la Seguridad.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void openModule(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = (Stage) tblComisiones.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void abrirPerfil() {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/coolbox/sistema/Vistas/Modales/PerfilUsuario.fxml"));
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Mi Perfil");
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
            String nombre = SesionUsuario.getNombreUsuario();
            lblNombreUsuarioSidebar.setText(nombre != null ? nombre : "");
        }
    }

}