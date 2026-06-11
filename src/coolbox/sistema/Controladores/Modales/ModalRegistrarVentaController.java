package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Producto;
import coolbox.sistema.Modelos.SaleItem;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Controladores.SesionUsuario; // 🌟 Acoplado perfectamente a tu paquete real
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // 🌟 IMPORTANTE: Requerido para recuperar las llaves autogeneradas
import java.util.ArrayList;
import java.util.List;

public class ModalRegistrarVentaController {

    @FXML private ComboBox<Tienda> cmbTienda;
    @FXML private ComboBox<Empleado> cmbEmpleado;
    
    // PASARELA DE PAGOS E INTEGRACIÓN DE INTERFAZ
    @FXML private ComboBox<String> cmbTipoPago;
    @FXML private ComboBox<String> cmbTipoTarjeta;
    @FXML private VBox paneEfectivo;
    
    @FXML private ComboBox<String> cmbTipoGarantia; 
    @FXML private ComboBox<SaleItem> cmbProductoParaGarantia; 
    
    @FXML private TextField txtGarantias;
    @FXML private TextField txtMontoPagado;
    @FXML private TextField txtIdProductoInput;
    
    @FXML private Label lblNombreProductoDetectado;
    @FXML private Label lblPrecioProductoDetectado;
    @FXML private Label lblTotalProductos;
    @FXML private Label lblTotalMonto;
    @FXML private Label lblVuelto;

    @FXML private TableView<SaleItem> tblDetalleVenta;
    @FXML private TableColumn<SaleItem, Integer> colIdProd;
    @FXML private TableColumn<SaleItem, String> colDescripcion;
    @FXML private TableColumn<SaleItem, Double> colPrecio;

    @FXML private Button btnGuardar;
    @FXML private Button btnImprimir;
    @FXML private Button btnCancelar;

    private Producto productoActual;
    private final List<SaleItem> detalle = new ArrayList<>();
    private double totalVentaGeneral = 0.0;
    
    // Almacena la boleta calculada para el ticket de impresión
    private String boletaGeneradaActual = "Pendiente";

    @FXML
    private void initialize() {
        loadTiendas();
        loadEmpleados();
        setupColumns();
        inicializarLogicaGarantias();
        inicializarPasarelaPagos();
        updateResumen();
        
        // 🔒 FLUJO PROTEGIDO: El botón de imprimir nace bloqueado hasta confirmar el registro en BD
        btnImprimir.setDisable(true);
    }

    private void setupColumns() {
        colIdProd.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
    }

    private void inicializarPasarelaPagos() {
        cmbTipoPago.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta de Crédito/Débito"));
        cmbTipoTarjeta.setItems(FXCollections.observableArrayList("Visa", "Mastercard", "American Express", "Diners Club"));
        
        cmbTipoTarjeta.setDisable(true);
        cmbTipoPago.setValue("Efectivo");

        cmbTipoPago.setOnAction(e -> {
            boolean esTarjeta = cmbTipoPago.getValue().contains("Tarjeta");
            cmbTipoTarjeta.setDisable(!esTarjeta);
            paneEfectivo.setDisable(esTarjeta);
            if (esTarjeta) {
                txtMontoPagado.setText(String.format("%.2f", totalVentaGeneral));
                lblVuelto.setText("S/ 0.00");
                cmbTipoTarjeta.requestFocus();
            } else {
                txtMontoPagado.setText("0.00");
                lblVuelto.setText("S/ 0.00");
            }
        });
    }

    private void inicializarLogicaGarantias() {
        cmbTipoGarantia.setItems(FXCollections.observableArrayList(
            "Garantía Clásica (1 Año - 10%)",
            "Garantía Extendida Plus (2 Años - 20%)",
            "Garantía Premium (1 Año + Robo - 40%)"
        ));
        cmbTipoGarantia.setOnAction(e -> calcularMontoGarantia());
        cmbProductoParaGarantia.setOnAction(e -> calcularMontoGarantia());
    }

    private void calcularMontoGarantia() {
        String tipo = cmbTipoGarantia.getValue();
        SaleItem itemSeleccionado = cmbProductoParaGarantia.getValue();
        if (tipo == null || itemSeleccionado == null) {
            txtGarantias.setText("0.00");
            return;
        }
        double porcentaje = tipo.contains("10%") ? 0.10 : tipo.contains("20%") ? 0.20 : 0.40;
        txtGarantias.setText(String.format("%.2f", itemSeleccionado.getPrecio() * porcentaje));
    }

    @FXML
    private void calcularVueltoDinámico() {
        try {
            double pagado = Double.parseDouble(txtMontoPagado.getText().trim());
            double vuelto = pagado - totalVentaGeneral;
            lblVuelto.setText(vuelto >= 0 ? String.format("S/ %.2f", vuelto) : "Monto insuficiente");
        } catch (Exception e) {
            lblVuelto.setText("S/ 0.00");
        }
    }

    @FXML
    private void aplicarGarantiaComoProducto() {
        double montoGarantia = 0;
        try { montoGarantia = Double.parseDouble(txtGarantias.getText().trim()); } catch (Exception ignored) {}
        if (montoGarantia <= 0 || cmbTipoGarantia.getValue() == null || cmbProductoParaGarantia.getValue() == null) return;

        SaleItem itemGarantia = new SaleItem();
        itemGarantia.setIdProducto(900 + cmbTipoGarantia.getSelectionModel().getSelectedIndex() + 1);
        itemGarantia.setDescripcion(cmbTipoGarantia.getValue() + " -> " + cmbProductoParaGarantia.getValue().getDescripcion());
        itemGarantia.setPrecio(montoGarantia);

        detalle.add(itemGarantia);
        tblDetalleVenta.setItems(FXCollections.observableArrayList(detalle));
        updateResumen();
        cmbTipoGarantia.setValue(null);
        txtGarantias.setText("0.00");
    }

    private void updateResumen() {
        totalVentaGeneral = detalle.stream().mapToDouble(SaleItem::getPrecio).sum();
        lblTotalProductos.setText(String.valueOf(detalle.size()));
        lblTotalMonto.setText(String.format("S/ %.2f", totalVentaGeneral)); 
        if ("Efectivo".equals(cmbTipoPago.getValue())) {
            calcularVueltoDinámico();
        }
    }

    private String obtenerSiguienteNumeroBoleta() {
        String sql = "SELECT TOP 1 numero_boleta FROM VENTAS WHERE numero_boleta LIKE 'B001-%' ORDER BY id_venta DESC";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String ultimoCorrelativo = rs.getString("numero_boleta");
                int numeroSiguiente = Integer.parseInt(ultimoCorrelativo.substring(5)) + 1;
                return String.format("B001-%08d", numeroSiguiente);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Primera boleta de la serie. Iniciando contador.");
        }
        return "B001-00000001";
    }

    // 🌟 REFACTORIZACIÓN COMPLETA MAESTRO-DETALLE (CON TRANSACCIONES FIABLES)
    @FXML
    private void guardarVentaEImprimir() {
        Tienda tienda = cmbTienda.getValue();
        Empleado empleado = cmbEmpleado.getValue();
        if (tienda == null || empleado == null || detalle.isEmpty()) return;

        double pagado = totalVentaGeneral;
        double vuelto = 0.0;

        if ("Efectivo".equals(cmbTipoPago.getValue())) {
            try {
                pagado = Double.parseDouble(txtMontoPagado.getText().trim());
                vuelto = pagado - totalVentaGeneral;
                if (vuelto < 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "⛔ Error en caja: El monto ingresado en efectivo es inferior al total.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
            } catch (Exception e) { return; }
        } else {
            if (cmbTipoTarjeta.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "⚠️ Seleccione la pasarela de la tarjeta (Visa, Mastercard, etc.).", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }

        boletaGeneradaActual = obtenerSiguienteNumeroBoleta();

        String sqlVenta = "INSERT INTO VENTAS(id_tienda, id_empleado, fecha, monto, productos_por_venta, tipo_pago, tarjeta_tipo, monto_pagado, vuelto, numero_boleta) " +
                          "VALUES(?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?, ?, ?, ?)";
                          
        String sqlDetalle = "INSERT INTO DETALLE_VENTA(id_venta, nombre_producto, cantidad, precio_unitario, subtotal, numero_boleta, fecha) VALUES(?, ?, ?, ?, ?, ?, CAST(GETDATE() AS DATE))";

        Connection connection = null;
        try {
            connection = ConexionDB.getConnection();
            connection.setAutoCommit(false); // 🔒 INICIO DE TRANSACCIÓN: Protege la base de datos contra registros incompletos

            // 1. Insertar Cabecera (Ventas) y solicitar el ID generado automáticamente por SQL Server
            try (PreparedStatement stmtVenta = connection.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                stmtVenta.setInt(1, tienda.getIdTienda());
                stmtVenta.setInt(2, empleado.getId());
                stmtVenta.setDouble(3, totalVentaGeneral);
                stmtVenta.setInt(4, detalle.size());
                stmtVenta.setString(5, cmbTipoPago.getValue());
                stmtVenta.setString(6, cmbTipoTarjeta.getValue());
                stmtVenta.setDouble(7, pagado);
                stmtVenta.setDouble(8, vuelto);
                stmtVenta.setString(9, boletaGeneradaActual);
                stmtVenta.executeUpdate();

                // Recuperamos el ID autogenerado
                int idVentaGenerado = -1;
                try (ResultSet generatedKeys = stmtVenta.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idVentaGenerado = generatedKeys.getInt(1);
                    }
                }

                if (idVentaGenerado == -1) {
                    throw new SQLException("❌ Fallo crítico: No se pudo obtener el ID autogenerado de la tabla VENTAS.");
                }

                // 2. Insertar cada producto del carrito en la tabla DETALLE_VENTA amarrados a ese ID
                try (PreparedStatement stmtDetalle = connection.prepareStatement(sqlDetalle)) {
                    for (SaleItem item : detalle) {
                        stmtDetalle.setInt(1, idVentaGenerado);
                        stmtDetalle.setString(2, item.getDescripcion());
                        stmtDetalle.setInt(3, 1); // Manejamos cantidad unitaria por fila
                        stmtDetalle.setDouble(4, item.getPrecio());
                        stmtDetalle.setDouble(5, item.getPrecio()); // Subtotal igual al precio
                        stmtDetalle.setString(6, boletaGeneradaActual);
                        stmtDetalle.addBatch(); // Empaqueta para inserción masiva veloz
                    }
                    stmtDetalle.executeBatch(); // Inserta todos los productos de golpe
                }
            }

            connection.commit(); // 🔓 CONFIRMACIÓN: Se guardan todos los cambios físicos solo si todo fue exitoso
            
            btnImprimir.setDisable(false);
            btnGuardar.setDisable(true);
            btnGuardar.setText("¡Venta Guardada!");
            txtIdProductoInput.setDisable(true);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "💾 ¡Venta y desglose de productos registrados exitosamente!\n\nNúmero de Comprobante: " + boletaGeneradaActual + "\n\nYa puede proceder a imprimir el Ticket.", ButtonType.OK);
            alert.showAndWait();

        } catch (SQLException e) {
            // Si cualquier proceso falla, se cancela todo para mantener la integridad de las tablas
            if (connection != null) {
                try {
                    System.err.println("⚠️ Ocurrió un error. Revirtiendo transacción (Rollback)...");
                    connection.rollback();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "⛔ Error de Consistencia: No se pudo guardar la venta. Inténtelo de nuevo.", ButtonType.OK);
            alert.showAndWait();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void imprimirTicketPDF() {
        if (detalle.isEmpty()) return;
        String rutaArchivo = System.getProperty("user.home") + "/Downloads/Ticket_" + boletaGeneradaActual + ".txt";
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write("=========================================\n");
            writer.write("         COOLBOX - TICKET DE VENTA       \n");
            writer.write("=========================================\n");
            writer.write("BOLETA ELECTRÓNICA: " + boletaGeneradaActual + "\n");
            writer.write("Sede: " + (cmbTienda.getValue() != null ? cmbTienda.getValue().getNombreTienda() : "General") + "\n");
            writer.write("Atendido por: " + (cmbEmpleado.getValue() != null ? cmbEmpleado.getValue().getNombres() : "Sistema") + "\n");
            writer.write("Metodo de pago: " + cmbTipoPago.getValue() + "\n");
            if (cmbTipoTarjeta.getValue() != null) writer.write("Tarjeta: " + cmbTipoTarjeta.getValue() + "\n");
            writer.write("-----------------------------------------\n");
            for (SaleItem item : detalle) {
                writer.write(String.format("%-28s S/ %6.2f\n", item.getDescripcion(), item.getPrecio()));
            }
            writer.write("-----------------------------------------\n");
            writer.write(String.format("TOTAL COMPRA:               S/ %6.2f\n", totalVentaGeneral));
            writer.write("=========================================\n");
            writer.write("    ¡Gracias por su compra en Coolbox!   \n");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "🖨️ Ticket fiscal generado en descargas:\n" + rutaArchivo, ButtonType.OK);
            alert.setTitle("Impresión Completada");
            alert.showAndWait();
            
            closeWindow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void buscarProductoEnBD() {
        String entrada = txtIdProductoInput.getText();
        if (entrada == null || entrada.isBlank()) return;
        try {
            int idProducto = Integer.parseInt(entrada.trim());
            String sql = "SELECT id_producto, nombre, precio FROM PRODUCTOS WHERE id_producto = ?";
            try (Connection connection = ConexionDB.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, idProducto);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        productoActual = new Producto();
                        productoActual.setIdProducto(rs.getInt("id_producto"));
                        productoActual.setDescripcion(rs.getString("nombre"));
                        productoActual.setPrecio(rs.getDouble("precio"));
                        lblNombreProductoDetectado.setText(productoActual.getDescripcion());
                        lblPrecioProductoDetectado.setText(String.format("S/ %.2f", productoActual.getPrecio()));
                        return;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        productoActual = null;
        lblNombreProductoDetectado.setText("Producto no encontrado");
        lblPrecioProductoDetectado.setText("S/ 0.00");
    }

    @FXML
    private void agregarItemATabla() {
        if (productoActual == null) return;
        SaleItem item = new SaleItem();
        item.setIdProducto(productoActual.getIdProducto());
        item.setDescripcion(productoActual.getDescripcion());
        item.setPrecio(productoActual.getPrecio());
        detalle.add(item);
        var listaObservable = FXCollections.observableArrayList(detalle);
        tblDetalleVenta.setItems(listaObservable);
        cmbProductoParaGarantia.setItems(listaObservable);
        updateResumen();
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

    private void loadEmpleados() {
        String sql = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
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
            if (idEmpleadoSesion > 0) {
                for (Empleado emp : empleados) {
                    if (emp.getId() == idEmpleadoSesion) {
                        cmbEmpleado.setValue(emp);
                        break;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void cerrarModal() { closeWindow(); }
    private void closeWindow() { ((Stage) btnCancelar.getScene().getWindow()).close(); }
}