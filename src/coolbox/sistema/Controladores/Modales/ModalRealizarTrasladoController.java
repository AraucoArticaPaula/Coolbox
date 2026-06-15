package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Producto;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalRealizarTrasladoController {

    @FXML
    private ComboBox<Tienda> cmbTiendaOrigen;
    @FXML
    private ComboBox<Tienda> cmbTiendaDestino;
    @FXML
    private ComboBox<Producto> cmbProducto;
    @FXML
    private TextField txtCantidad;
    @FXML
    private ComboBox<Empleado> cmbEmpleado;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    private int idTiendaUsuario = -1;

    @FXML
    private void initialize() {
        obtenerTiendaDeSesion();
        loadTiendas();
        loadProductos();
        loadEmpleados();
        restringirAccesosPorRol();
    }

    private void obtenerTiendaDeSesion() {
        String sql = "SELECT id_tienda FROM EMPLEADOS e JOIN USUARIOS u ON e.id_empleado = u.id_empleado WHERE u.nombre_usuario = ?";
        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, SesionUsuario.getNombreUsuario());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idTiendaUsuario = rs.getInt("id_tienda");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTiendas() {

        String sql = "SELECT id_tienda, nombre_tienda, centro_comercial FROM TIENDAS";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {

            var tiendas = FXCollections.<Tienda>observableArrayList();
            while (rs.next()) {
                Tienda tienda = new Tienda();
                tienda.setIdTienda(rs.getInt("id_tienda"));
                tienda.setNombreTienda(rs.getString("nombre_tienda"));
                tienda.setCentroComercial(rs.getString("centro_comercial"));

                tiendas.add(tienda);
            }

            cmbTiendaOrigen.setItems(tiendas);
            cmbTiendaDestino.setItems(tiendas);

            if (!"ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {
                for (Tienda t : tiendas) {
                    if (t.getIdTienda() == idTiendaUsuario) {
                        cmbTiendaOrigen.setValue(t);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error de mapeo en la tabla TIENDAS.");
            e.printStackTrace();
        }
    }

    private void loadProductos() {
        String sql = "SELECT id_producto, nombre, precio FROM PRODUCTOS";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            var productos = FXCollections.<Producto>observableArrayList();
            while (rs.next()) {
                Producto producto = new Producto();
                producto.setIdProducto(rs.getInt("id_producto"));
                producto.setDescripcion(rs.getString("nombre"));
                producto.setPrecio(rs.getDouble("precio"));
                productos.add(producto);
            }
            cmbProducto.setItems(productos);
        } catch (SQLException ignored) {
        }
    }

    private void loadEmpleados() {
        StringBuilder sql = new StringBuilder(
                "SELECT e.id_empleado, e.nombres, e.apellidos FROM EMPLEADOS e "
                + "INNER JOIN EMPLEADOS_CARGOS ec ON e.id_empleado = ec.id_empleado "
                + "INNER JOIN CARGOS c ON ec.id_cargo = c.id_cargo "
                + "WHERE c.nombre_cargo IN ('Gerente', 'Subgerente')"
        );

        if (!"ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {
            sql.append(" AND e.id_tienda = ").append(idTiendaUsuario);
        }

        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString()); ResultSet rs = statement.executeQuery()) {
            var empleados = FXCollections.<Empleado>observableArrayList();
            while (rs.next()) {
                Empleado empleado = new Empleado();
                empleado.setId(rs.getInt("id_empleado"));
                empleado.setNombres(rs.getString("nombres"));
                empleado.setApellidos(rs.getString("apellidos"));
                empleados.add(empleado);
            }
            cmbEmpleado.setItems(empleados);
        } catch (SQLException ignored) {
        }
    }

    private void restringirAccesosPorRol() {
        if (!"ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {
            cmbTiendaOrigen.setDisable(true);
        }
    }

    @FXML
    private void guardarTraslado() {
        Tienda origen = cmbTiendaOrigen.getValue();
        Tienda destino = cmbTiendaDestino.getValue();
        Producto producto = cmbProducto.getValue();
        Empleado empleado = cmbEmpleado.getValue();
        int cantidad = parseInteger(txtCantidad.getText());

        if (origen == null || destino == null || producto == null || empleado == null || cantidad <= 0 || origen.getIdTienda() == destino.getIdTienda()) {
            return;
        }

        try (Connection connection = ConexionDB.getConnection()) {
            connection.setAutoCommit(false);

            int sourceInventoryId = getInventarioId(connection, origen.getIdTienda(), producto.getIdProducto());
            int sourceStock = getStockActual(connection, origen.getIdTienda(), producto.getIdProducto());

            if (sourceInventoryId < 0 || sourceStock < cantidad) {
                System.err.println("❌ Stock insuficiente en la sucursal de origen.");
                return;
            }

            int newSourceStock = sourceStock - cantidad;
            updateInventario(connection, sourceInventoryId, newSourceStock);

            saveTraslado(connection, producto.getIdProducto(), origen.getIdTienda(), destino.getIdTienda(), cantidad, empleado.getId());

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        closeWindow();
    }

    private int getInventarioId(Connection connection, int idTienda, int idProducto) throws SQLException {
        String sql = "SELECT id_inventario FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idTienda);
            statement.setInt(2, idProducto);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_inventario");
                }
            }
        }
        return -1;
    }

    private int getStockActual(Connection connection, int idTienda, int idProducto) throws SQLException {
        String sql = "SELECT stock_actual FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idTienda);
            statement.setInt(2, idProducto);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_actual");
                }
            }
        }
        return 0;
    }

    private void updateInventario(Connection connection, int idInventario, int stockActual) throws SQLException {
        String sql = "UPDATE INVENTARIO SET stock_actual = ?, fecha_inventario = CAST(GETDATE() AS DATE) WHERE id_inventario = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockActual);
            statement.setInt(2, idInventario);
            statement.executeUpdate();
        }
    }

    private void saveTraslado(Connection connection, int idProducto, int origen, int destino, int cantidad, int idEmpleado) throws SQLException {
        String sql = "INSERT INTO TRASLADOS(id_producto, id_tienda_origen, id_tienda_destino, cantidad, id_empleado, fecha, estado) "
                + "VALUES(?, ?, ?, ?, ?, CAST(GETDATE() AS DATE), 'Pendiente')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idProducto);
            statement.setInt(2, origen);
            statement.setInt(3, destino);
            statement.setInt(4, cantidad);
            statement.setInt(5, idEmpleado);
            statement.executeUpdate();
        }
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
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
