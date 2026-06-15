package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Producto;
import coolbox.sistema.Modelos.SaleItem;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Controladores.SesionUsuario;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ModalRegistrarVentaController {

    @FXML
    private ComboBox<Tienda> cmbTienda;
    @FXML
    private ComboBox<Empleado> cmbEmpleado;

    @FXML
    private ComboBox<String> cmbTipoPago;
    @FXML
    private ComboBox<String> cmbTipoTarjeta;
    @FXML
    private VBox paneEfectivo;

    @FXML
    private ComboBox<String> cmbTipoGarantia;
    @FXML
    private ComboBox<SaleItem> cmbProductoParaGarantia;

    @FXML
    private TextField txtGarantias;
    @FXML
    private TextField txtMontoPagado;
    @FXML
    private TextField txtIdProductoInput;

    @FXML
    private Label lblNombreProductoDetectado;
    @FXML
    private Label lblPrecioProductoDetectado;
    @FXML
    private Label lblTotalProductos;
    @FXML
    private Label lblTotalMonto;
    @FXML
    private Label lblVuelto;

    @FXML
    private TableView<SaleItem> tblDetalleVenta;
    @FXML
    private TableColumn<SaleItem, Integer> colIdProd;
    @FXML
    private TableColumn<SaleItem, String> colDescripcion;
    @FXML
    private TableColumn<SaleItem, Double> colPrecio;

    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnImprimir;
    @FXML
    private Button btnCancelar;

    private Producto productoActual;
    private final List<SaleItem> detalle = new ArrayList<>();
    private double totalVentaGeneral = 0.0;

    private String boletaGeneradaActual = "Pendiente";

    private static final int ID_GARANTIA_BASE = 900;

    @FXML
    private void initialize() {
        loadTiendas();
        loadEmpleados();
        setupColumns();
        inicializarLogicaGarantias();
        inicializarPasarelaPagos();
        updateResumen();
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
        try {
            montoGarantia = Double.parseDouble(txtGarantias.getText().trim());
        } catch (Exception ignored) {
        }
        if (montoGarantia <= 0 || cmbTipoGarantia.getValue() == null || cmbProductoParaGarantia.getValue() == null) {
            return;
        }

        SaleItem itemGarantia = new SaleItem();
        // IDs de garantía: 901, 902, 903 — nunca coinciden con productos reales
        itemGarantia.setIdProducto(ID_GARANTIA_BASE + cmbTipoGarantia.getSelectionModel().getSelectedIndex() + 1);
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
        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
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

    private boolean verificarStockDisponible(Connection con, int idTienda) throws SQLException {
        String sqlStock = "SELECT stock_actual FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement stmt = con.prepareStatement(sqlStock)) {
            for (SaleItem item : detalle) {
                if (item.getIdProducto() >= ID_GARANTIA_BASE) {
                    continue;
                }

                stmt.setInt(1, idTienda);
                stmt.setInt(2, item.getIdProducto());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next() || rs.getInt("stock_actual") < 1) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "⚠️ Sin stock disponible para: " + item.getDescripcion()
                                + "\nVerifique el inventario de esta tienda.", ButtonType.OK);
                        alert.showAndWait();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void descontarStockInventario(Connection con, int idTienda) throws SQLException {
        String sqlInventario
                = "UPDATE INVENTARIO SET stock_actual = stock_actual - 1 "
                + "WHERE id_tienda = ? AND id_producto = ?";

        String sqlProducto
                = "UPDATE PRODUCTOS SET stock = stock - 1 "
                + "WHERE id_producto = ?";

        try (PreparedStatement stmtInv = con.prepareStatement(sqlInventario); PreparedStatement stmtProd = con.prepareStatement(sqlProducto)) {

            for (SaleItem item : detalle) {
                if (item.getIdProducto() >= ID_GARANTIA_BASE) {
                    continue;
                }

                stmtInv.setInt(1, idTienda);
                stmtInv.setInt(2, item.getIdProducto());
                stmtInv.addBatch();

                stmtProd.setInt(1, item.getIdProducto());
                stmtProd.addBatch();
            }

            stmtInv.executeBatch();
            stmtProd.executeBatch();
        }
    }

    @FXML
    private void guardarVentaEImprimir() {
        Tienda tienda = cmbTienda.getValue();
        Empleado empleado = cmbEmpleado.getValue();
        if (tienda == null || empleado == null || detalle.isEmpty()) {
            return;
        }

        double pagado = totalVentaGeneral;
        double vuelto = 0.0;

        if ("Efectivo".equals(cmbTipoPago.getValue())) {
            try {
                pagado = Double.parseDouble(txtMontoPagado.getText().trim());
                vuelto = pagado - totalVentaGeneral;
                if (vuelto < 0) {
                    new Alert(Alert.AlertType.ERROR,
                            "⛔ Error en caja: El monto en efectivo es inferior al total.", ButtonType.OK)
                            .showAndWait();
                    return;
                }
            } catch (Exception e) {
                return;
            }
        } else {
            if (cmbTipoTarjeta.getValue() == null) {
                new Alert(Alert.AlertType.WARNING,
                        "⚠️ Seleccione la pasarela de la tarjeta (Visa, Mastercard, etc.).", ButtonType.OK)
                        .showAndWait();
                return;
            }
        }

        boletaGeneradaActual = obtenerSiguienteNumeroBoleta();

        String sqlVenta
                = "INSERT INTO VENTAS(id_tienda, id_empleado, fecha, monto, productos_por_venta, "
                + "tipo_pago, tarjeta_tipo, monto_pagado, vuelto, numero_boleta) "
                + "VALUES(?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?, ?, ?, ?)";

        String sqlDetalle
                = "INSERT INTO DETALLE_VENTA(id_venta, id_producto, nombre_producto, cantidad, "
                + "precio_unitario, subtotal, numero_boleta, fecha) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, CAST(GETDATE() AS DATE))";

        Connection connection = null;
        try {
            connection = ConexionDB.getConnection();
            connection.setAutoCommit(false);

            if (!verificarStockDisponible(connection, tienda.getIdTienda())) {
                connection.rollback();
                return;
            }

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

                int idVentaGenerado = -1;
                try (ResultSet generatedKeys = stmtVenta.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idVentaGenerado = generatedKeys.getInt(1);
                    }
                }

                if (idVentaGenerado == -1) {
                    throw new SQLException("❌ No se pudo obtener el ID autogenerado de VENTAS.");
                }

                try (PreparedStatement stmtDetalle = connection.prepareStatement(sqlDetalle)) {
                    for (SaleItem item : detalle) {
                        stmtDetalle.setInt(1, idVentaGenerado);
                        stmtDetalle.setInt(2, item.getIdProducto());
                        stmtDetalle.setString(3, item.getDescripcion());
                        stmtDetalle.setInt(4, 1);
                        stmtDetalle.setDouble(5, item.getPrecio());
                        stmtDetalle.setDouble(6, item.getPrecio());
                        stmtDetalle.setString(7, boletaGeneradaActual);
                        stmtDetalle.addBatch();
                    }
                    stmtDetalle.executeBatch();
                }
            }

            descontarStockInventario(connection, tienda.getIdTienda());

            connection.commit();

            btnImprimir.setDisable(false);
            btnGuardar.setDisable(true);
            btnGuardar.setText("¡Venta Guardada!");
            txtIdProductoInput.setDisable(true);

            new Alert(Alert.AlertType.INFORMATION,
                    "💾 ¡Venta registrada y stock actualizado exitosamente!\n\n"
                    + "Comprobante: " + boletaGeneradaActual + "\n\n"
                    + "Ya puede proceder a imprimir el Ticket.", ButtonType.OK)
                    .showAndWait();

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    System.err.println("⚠️ Error detectado. Revirtiendo transacción (Rollback)...");
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "⛔ Error: No se pudo guardar la venta.\n" + e.getMessage(), ButtonType.OK)
                    .showAndWait();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void imprimirTicketPDF() {
        if (detalle.isEmpty()) {
            return;
        }
        String rutaArchivo = System.getProperty("user.home") + "/Downloads/Ticket_" + boletaGeneradaActual + ".txt";
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write("=========================================\n");
            writer.write("         COOLBOX - TICKET DE VENTA       \n");
            writer.write("=========================================\n");
            writer.write("BOLETA ELECTRÓNICA: " + boletaGeneradaActual + "\n");
            writer.write("Sede: " + (cmbTienda.getValue() != null ? cmbTienda.getValue().getNombreTienda() : "General") + "\n");
            writer.write("Atendido por: " + (cmbEmpleado.getValue() != null ? cmbEmpleado.getValue().getNombres() : "Sistema") + "\n");
            writer.write("Metodo de pago: " + cmbTipoPago.getValue() + "\n");
            if (cmbTipoTarjeta.getValue() != null) {
                writer.write("Tarjeta: " + cmbTipoTarjeta.getValue() + "\n");
            }
            writer.write("-----------------------------------------\n");
            for (SaleItem item : detalle) {
                writer.write(String.format("%-28s S/ %6.2f\n", item.getDescripcion(), item.getPrecio()));
            }
            writer.write("-----------------------------------------\n");
            writer.write(String.format("TOTAL COMPRA:               S/ %6.2f\n", totalVentaGeneral));
            writer.write("=========================================\n");
            writer.write("    ¡Gracias por su compra en Coolbox!   \n");

            new Alert(Alert.AlertType.INFORMATION,
                    "🖨️ Ticket generado en descargas:\n" + rutaArchivo)
                    .showAndWait();

            closeWindow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void buscarProductoEnBD() {
        String entrada = txtIdProductoInput.getText();
        if (entrada == null || entrada.isBlank()) {
            return;
        }
        try {
            int idProducto = Integer.parseInt(entrada.trim());
            String sql = "SELECT p.id_producto, p.nombre, p.precio, "
                    + "ISNULL(i.stock_actual, 0) AS stock_tienda "
                    + "FROM PRODUCTOS p "
                    + "LEFT JOIN INVENTARIO i ON i.id_producto = p.id_producto AND i.id_tienda = ? "
                    + "WHERE p.id_producto = ?";
            try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                int idTienda = cmbTienda.getValue() != null ? cmbTienda.getValue().getIdTienda() : 0;
                statement.setInt(1, idTienda);
                statement.setInt(2, idProducto);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        productoActual = new Producto();
                        productoActual.setIdProducto(rs.getInt("id_producto"));
                        productoActual.setDescripcion(rs.getString("nombre"));
                        productoActual.setPrecio(rs.getDouble("precio"));
                        int stockTienda = rs.getInt("stock_tienda");
                        lblNombreProductoDetectado.setText(productoActual.getDescripcion());
                        lblPrecioProductoDetectado.setText(
                                String.format("S/ %.2f  |  Stock tienda: %d", productoActual.getPrecio(), stockTienda));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        productoActual = null;
        lblNombreProductoDetectado.setText("Producto no encontrado");
        lblPrecioProductoDetectado.setText("S/ 0.00");
    }

    @FXML
    private void agregarItemATabla() {
        if (productoActual == null) {
            return;
        }
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
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {

            var tiendas = FXCollections.<Tienda>observableArrayList();
            Tienda tiendaUsuarioActual = null;

            int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
            String rolUsuario = SesionUsuario.getRolUsuario() != null
                    ? SesionUsuario.getRolUsuario().toUpperCase() : "INVITADO";

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

            boolean esAdmin = "ADMINISTRADOR".equals(rolUsuario) || "ADMIN".equals(rolUsuario);
            cmbTienda.setDisable(!esAdmin);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadEmpleados() {
        String sql = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cerrarModal() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
