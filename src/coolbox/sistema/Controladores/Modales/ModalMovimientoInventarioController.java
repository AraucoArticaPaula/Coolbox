package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Producto;
import coolbox.sistema.Modelos.Tienda;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalMovimientoInventarioController {

    @FXML private ComboBox<Tienda> cmbTienda;
    @FXML private ComboBox<String> cmbTipoMovimiento;
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    @FXML
    private void initialize() {
        loadTiendas();
        loadProductos();
        cmbTipoMovimiento.setItems(FXCollections.observableArrayList("Alta", "Baja"));
    }

    private void loadTiendas() {
        String sql = "SELECT id_tienda, nombre_tienda, centro_comercial FROM TIENDAS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            var tiendas = FXCollections.<Tienda>observableArrayList();
            while (rs.next()) {
                Tienda tienda = new Tienda();
                tienda.setIdTienda(rs.getInt("id_tienda"));
                tienda.setNombreTienda(rs.getString("nombre_tienda"));
                tienda.setCentroComercial(rs.getString("centro_comercial"));
                tiendas.add(tienda);
            }
            cmbTienda.setItems(tiendas);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadProductos() {
        String sql = "SELECT id_producto, nombre, precio FROM PRODUCTOS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            var productos = FXCollections.<Producto>observableArrayList();
            while (rs.next()) {
                Producto producto = new Producto();
                producto.setIdProducto(rs.getInt("id_producto"));
                producto.setDescripcion(rs.getString("nombre"));
                producto.setPrecio(rs.getDouble("precio"));
                productos.add(producto);
            }
            cmbProducto.setItems(productos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarMovimiento() {
        Tienda tienda = cmbTienda.getValue();
        Producto producto = cmbProducto.getValue();
        String tipo = cmbTipoMovimiento.getValue();
        int cantidad = parseInteger(txtCantidad.getText());
        String motivo = txtMotivo.getText();

        if (tienda == null || producto == null || tipo == null || cantidad <= 0) return;

        try (Connection connection = ConexionDB.getConnection()) {
            connection.setAutoCommit(false);

            int idInventario = getInventarioId(connection, tienda.getIdTienda(), producto.getIdProducto());
            
            if (idInventario > 0) {
                int stockActualInventario = getStockActual(connection, idInventario);
                int nuevoStockInv = "Alta".equalsIgnoreCase(tipo) ? stockActualInventario + cantidad : stockActualInventario - cantidad;
                if (nuevoStockInv < 0) {
                    System.err.println("❌ Stock insuficiente en el almacén de esta tienda.");
                    return;
                }
                updateInventario(connection, idInventario, nuevoStockInv);
            } else if ("Alta".equalsIgnoreCase(tipo)) {
                idInventario = createInventario(connection, tienda.getIdTienda(), producto.getIdProducto(), cantidad);
            } else {
                System.err.println("❌ No se puede realizar una baja sin inventario previo en esta tienda.");
                return;
            }
            
            int stockGlobalActual = getStockGlobalProducto(connection, producto.getIdProducto());
            int nuevoStockGlobal = "Alta".equalsIgnoreCase(tipo) ? stockGlobalActual + cantidad : stockGlobalActual - cantidad;
            if (nuevoStockGlobal < 0) return;
            updateStockGlobalProducto(connection, producto.getIdProducto(), nuevoStockGlobal);

            String usuarioActivo = "Administrador"; 

            saveMovimiento(connection, idInventario, tienda.getIdTienda(), tipo.toLowerCase(), cantidad, motivo, usuarioActivo);
            
            connection.commit(); 
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveMovimiento(Connection connection, int idInventario, int idTienda, String tipo, int cantidad, String motivo, String nombreUsuario) throws SQLException {
        String sql = "INSERT INTO MOVIMIENTOS_INVENTARIO(id_inventario, id_tienda, tipo_movimiento, cantidad, motivo, nombre_usuario, fecha) " +
                     "VALUES(?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idInventario);
            stmt.setInt(2, idTienda);
            stmt.setString(3, tipo);
            stmt.setInt(4, cantidad);
            stmt.setString(5, motivo);
            stmt.setString(6, nombreUsuario); 
            stmt.executeUpdate();
        }
    }

    private int getInventarioId(Connection connection, int idTienda, int idProducto) throws SQLException {
        String sql = "SELECT id_inventario FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idTienda); stmt.setInt(2, idProducto);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getInt("id_inventario") : -1; }
        }
    }

    private int getStockActual(Connection connection, int idInventario) throws SQLException {
        String sql = "SELECT stock_actual FROM INVENTARIO WHERE id_inventario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idInventario);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getInt("stock_actual") : 0; }
        }
    }

    private void updateInventario(Connection connection, int idInventario, int nuevoStock) throws SQLException {
        String sql = "UPDATE INVENTARIO SET stock_actual = ?, fecha_inventario = GETDATE() WHERE id_inventario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nuevoStock); stmt.setInt(2, idInventario); stmt.executeUpdate();
        }
    }

    private int createInventario(Connection connection, int idTienda, int idProducto, int cantidad) throws SQLException {
        String sql = "INSERT INTO INVENTARIO(id_tienda, id_producto, stock_actual, fecha_inventario) VALUES(?, ?, ?, GETDATE())";
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idTienda); stmt.setInt(2, idProducto); stmt.setInt(3, cantidad); stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : -1; }
        }
    }

    private int getStockGlobalProducto(Connection connection, int idProducto) throws SQLException {
        String sql = "SELECT stock FROM PRODUCTOS WHERE id_producto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getInt("stock") : 0; }
        }
    }

    private void updateStockGlobalProducto(Connection connection, int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE PRODUCTOS SET stock = ? WHERE id_producto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nuevoStock); stmt.setInt(2, idProducto); stmt.executeUpdate();
        }
    }

    private int parseInteger(String value) { try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; } }
    
    // MÉTODOS DE CIERRE COMPATIBLES CON TU FXML (Evita el LoadException)
    @FXML private void cerrarModal() { closeWindow(); }
    private void closeWindow() { ((Stage) btnCancelar.getScene().getWindow()).close(); }
}