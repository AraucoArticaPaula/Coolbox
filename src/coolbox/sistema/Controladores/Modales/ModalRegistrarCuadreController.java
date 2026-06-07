package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Controladores.SesionUsuario; // Conexión nativa con tu clase de sesión
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea; // Mapeo nativo del componente multilínea
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ModalRegistrarCuadreController {

    @FXML private ComboBox<Tienda> cmbTienda;
    @FXML private ComboBox<Empleado> cmbEmpleado;
    @FXML private TextField txtMontoEsperado;
    @FXML private TextField txtMontoTotal; // Efectivo Físico en Caja
    
    // Componentes del formulario avanzado
    @FXML private TextField txtFondoCaja;
    @FXML private TextField txtMontoTarjeta;
    
    @FXML private TextArea txtObservaciones; 
    @FXML private TextField txtFechaCuadre; 
    
    @FXML private Label lblAlertaMensaje;
    @FXML private Label lblMontoDiferencia;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    // 🌟 CONSTANTE ARREGLADA: El fondo siempre nace en 100.00 soles
    private final double FONDO_FIJO_CAJA = 100.00;
    
    private double ventasTarjetaTotal = 0.0;
    private double ventasGlobalTotal = 0.0;
    private double efectivoEsperadoEnCaja = 0.0;

    private Runnable onGuardarSuccess;

    public void setOnGuardarSuccess(Runnable onGuardarSuccess) {
        this.onGuardarSuccess = onGuardarSuccess;
    }

    @FXML
    private void initialize() {
        inyectarFechaActual();
        loadTiendas();
        
        int idTiendaActiva = cmbTienda.getValue() != null ? cmbTienda.getValue().getIdTienda() : -1;
        loadEmpleados(idTiendaActiva);
        
        // Sincroniza las ventas de hoy y calcula el efectivo esperado basándose en el fondo estático
        consultarMetricasFinancieras();
        calcularDiferenciaCuadre();

        // LISTENER EN TIEMPO REAL
        txtMontoTotal.textProperty().addListener((observable, oldValue, newValue) -> {
            calcularDiferenciaCuadre();
        });
    }

    private void inyectarFechaActual() {
        LocalDate hoy = LocalDate.now();
        DateTimeFormatter formatoPeruano = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        txtFechaCuadre.setText(hoy.format(formatoPeruano));
        txtFechaCuadre.setEditable(false);
    }

    private void loadTiendas() {
        String sql = "SELECT id_tienda, nombre_tienda FROM TIENDAS"; 
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            var tiendas = FXCollections.<Tienda>observableArrayList();
            Tienda tiendaUsuarioActual = null;
            
            int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
            String rolUsuario = SesionUsuario.getRolUsuario() != null ? SesionUsuario.getRolUsuario().toUpperCase() : "INVITADO";

            while (rs.next()) {
                Tienda tienda = new Tienda();
                tienda.setIdTienda(rs.getInt("id_tienda"));
                tienda.setNombreTienda(rs.getString("nombre_tienda"));
                tiendas.add(tienda);
                
                if (idTiendaSesion > 0 && tienda.getIdTienda() == idTiendaSesion) {
                    tiendaUsuarioActual = tienda;
                }
            }
            cmbTienda.setItems(tiendas);
            
            if (tiendaUsuarioActual != null) {
                cmbTienda.setValue(tiendaUsuarioActual);
            } else if (!tiendas.isEmpty()) {
                cmbTienda.getSelectionModel().selectFirst();
            }

            if (!"ADMINISTRADOR".equals(rolUsuario) && !"ADMIN".equals(rolUsuario)) {
                cmbTienda.setDisable(true);
            } else {
                cmbTienda.setDisable(false);
            }
            
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadEmpleados(int idTienda) {
        if (idTienda <= 0) return;

        String sql = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_tienda = ?";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, idTienda);
            
            try (ResultSet rs = statement.executeQuery()) {
                var empleados = FXCollections.<Empleado>observableArrayList();
                while (rs.next()) {
                    Empleado empleado = new Empleado();
                    empleado.setId(rs.getInt("id_empleado"));
                    empleado.setNombres(rs.getString("nombres"));
                    empleado.setApellidos(rs.getString("apellidos"));
                    empleados.add(empleado);
                }
                cmbEmpleado.setItems(empleados);
                
                int idEmpleadoSesion = SesionUsuario.getIdEmpleado();
                String rolUsuario = SesionUsuario.getRolUsuario() != null ? SesionUsuario.getRolUsuario().toUpperCase() : "INVITADO";

                if (idEmpleadoSesion > 0) {
                    for (Empleado emp : empleados) {
                        if (emp.getId() == idEmpleadoSesion) {
                            cmbEmpleado.setValue(emp);
                            break;
                        }
                    }
                }

                if (!"ADMINISTRADOR".equals(rolUsuario) && !"ADMIN".equals(rolUsuario)) {
                    cmbEmpleado.setDisable(true); 
                } else {
                    cmbEmpleado.setDisable(false);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void consultarMetricasFinancieras() {
        Tienda tienda = cmbTienda.getValue();
        if (tienda == null) return;

        // 🌟 CORREGIDO: Se inyecta el fondo fijo de 100 soles directo en la interfaz sin tocar la BD
        txtFondoCaja.setText(String.format("%.2f", FONDO_FIJO_CAJA));

        try (Connection connection = ConexionDB.getConnection()) {
            // 1. Extraer la sumatoria Global de Ventas registradas hoy
            String sqlGlobal = "SELECT ISNULL(SUM(monto), 0) AS total FROM VENTAS WHERE id_tienda = ? AND CAST(fecha AS DATE) = CAST(GETDATE() AS DATE)";
            try (PreparedStatement stmtGlobal = connection.prepareStatement(sqlGlobal)) {
                stmtGlobal.setInt(1, tienda.getIdTienda());
                try (ResultSet rs = stmtGlobal.executeQuery()) {
                    if (rs.next()) { ventasGlobalTotal = rs.getDouble("total"); }
                }
            }
            txtMontoEsperado.setText(String.format("%.2f", ventasGlobalTotal));

            // 2. Extraer la sumatoria exclusiva de Ventas procesadas mediante Tarjetas
            String sqlTarjeta = "SELECT ISNULL(SUM(monto), 0) AS total_tarjeta FROM VENTAS WHERE id_tienda = ? AND tipo_pago LIKE '%Tarjeta%' AND CAST(fecha AS DATE) = CAST(GETDATE() AS DATE)";
            try (PreparedStatement stmtTarjeta = connection.prepareStatement(sqlTarjeta)) {
                stmtTarjeta.setInt(1, tienda.getIdTienda());
                try (ResultSet rs = stmtTarjeta.executeQuery()) {
                    if (rs.next()) { ventasTarjetaTotal = rs.getDouble("total_tarjeta"); }
                }
            }
            txtMontoTarjeta.setText(String.format("%.2f", ventasTarjetaTotal));

            // ⚙️ FÓRMULA CORREGIDA: Efectivo esperado = 100.00 + Ventas Totales del Día - Ventas Tarjeta
            efectivoEsperadoEnCaja = FONDO_FIJO_CAJA + (ventasGlobalTotal - ventasTarjetaTotal);

        } catch (SQLException e) {
            System.err.println("❌ Error al sincronizar métricas de arqueo diario.");
            e.printStackTrace();
        }
    }

    @FXML
    private void calcularDiferenciaCuadre() {
        double totalFisicoDeclarado = parseDouble(txtMontoTotal.getText());
        
        // Comparación matemática directa contra lo esperado en efectivo real
        double diferencia = totalFisicoDeclarado - efectivoEsperadoEnCaja;
        
        lblMontoDiferencia.setText(String.format("S/ %.2f", diferencia));
        
        if (Math.abs(diferencia) < 0.01) {
            lblAlertaMensaje.setText("✓ Efectivo cuadrado conforme.");
            lblAlertaMensaje.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
        } else if (diferencia < 0) {
            lblAlertaMensaje.setText(String.format("⚠️ DESCUADRE: Falta S/ %.2f en efectivo físico.", Math.abs(diferencia)));
            lblAlertaMensaje.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
        } else {
            lblAlertaMensaje.setText(String.format("💰 SOBRANTE: Exceso de S/ %.2f en caja física.", diferencia));
            lblAlertaMensaje.setStyle("-fx-text-fill: #0056B3; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void guardarCuadre() {
        Tienda tienda = cmbTienda.getValue();
        Empleado empleado = cmbEmpleado.getValue();
        if (tienda == null || empleado == null) return;

        double montoFisicoEfectivo = parseDouble(txtMontoTotal.getText());
        String observaciones = txtObservaciones.getText() != null ? txtObservaciones.getText().trim() : "";
        
        String sql = "INSERT INTO CUADRE_CAJA(id_tienda, id_empleado, fecha, monto_total, observaciones) VALUES(?, ?, CAST(GETDATE() AS DATE), ?, ?)";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tienda.getIdTienda());
            statement.setInt(2, empleado.getId());
            statement.setDouble(3, montoFisicoEfectivo); 
            statement.setString(4, observaciones);
            statement.executeUpdate();
            
            if (onGuardarSuccess != null) {
                javafx.application.Platform.runLater(onGuardarSuccess);
            }
            
            closeWindow();
            
        } catch (SQLException e) {
            System.err.println("❌ Error crítico al escribir en CUADRE_CAJA.");
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "⛔ Error de persistencia: El arqueo no pudo guardarse en la BD.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML private void cerrarModal() { closeWindow(); }
    private void closeWindow() { ((Stage) btnCancelar.getScene().getWindow()).close(); }
}