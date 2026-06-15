package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Traslado;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalRecibirTrasladoController {

    @FXML
    private TextField txtProducto;
    @FXML
    private TextField txtOrigen;
    @FXML
    private TextField txtCantidad;
    @FXML
    private ComboBox<Empleado> cmbResponsableDestino;
    @FXML
    private Button btnCancelar;

    private static Traslado trasladoSeleccionado;
    private int idTiendaUsuario = -1;

    public static void setTrasladoAProcesar(Traslado t) {
        trasladoSeleccionado = t;
    }

    @FXML
    private void initialize() {
        obtenerTiendaDeSesion();
        validarPermisoRecepcion();
        completarDatosVista();
        loadResponsablesDestino();
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

    private void validarPermisoRecepcion() {

        if (!"ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {

            if (idTiendaUsuario == -1) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "⛔ OPERACIÓN DENEGADA.\n\nNo se pudo verificar la sucursal de tu sesión actual.", ButtonType.OK);
                    alert.showAndWait();
                    closeWindow();
                });
            }
        }
    }

    private void completarDatosVista() {
        if (trasladoSeleccionado != null) {
            txtProducto.setText(trasladoSeleccionado.getProducto());
            txtOrigen.setText(trasladoSeleccionado.getOrigen());
            txtCantidad.setText(String.valueOf(trasladoSeleccionado.getCantidad()));
        }
    }

    private void loadResponsablesDestino() {

        if (idTiendaUsuario == -1) {
            return;
        }

        String sql = "SELECT e.id_empleado, e.nombres, e.apellidos FROM EMPLEADOS e "
                + "INNER JOIN EMPLEADOS_CARGOS ec ON e.id_empleado = ec.id_empleado "
                + "INNER JOIN CARGOS c ON ec.id_cargo = c.id_cargo "
                + "WHERE c.nombre_cargo IN ('Gerente', 'Subgerente') AND e.id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idTiendaUsuario);

            try (ResultSet rs = statement.executeQuery()) {
                var empleados = FXCollections.<Empleado>observableArrayList();
                while (rs.next()) {
                    Empleado empleado = new Empleado();
                    empleado.setId(rs.getInt("id_empleado"));
                    empleado.setNombres(rs.getString("nombres"));
                    empleado.setApellidos(rs.getString("apellidos"));
                    empleados.add(empleado);
                }
                cmbResponsableDestino.setItems(empleados);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al cargar encargados de recepción.");
            e.printStackTrace();
        }
    }

    @FXML
    private void confirmarRecepcion() {
        Empleado empleado = cmbResponsableDestino.getValue();
        if (empleado == null || trasladoSeleccionado == null || idTiendaUsuario == -1) {
            return;
        }

        int cantidad = trasladoSeleccionado.getCantidad();

        try (Connection connection = ConexionDB.getConnection()) {
            connection.setAutoCommit(false);

            int idProducto = -1;
            String sqlProd = "SELECT id_producto FROM PRODUCTOS WHERE nombre = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlProd)) {
                stmt.setString(1, trasladoSeleccionado.getProducto());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        idProducto = rs.getInt("id_producto");
                    }
                }
            }

            int destInventoryId = getInventarioId(connection, idTiendaUsuario, idProducto);
            if (destInventoryId > 0) {
                int destStock = getStockActual(connection, idTiendaUsuario, idProducto);
                updateInventario(connection, destInventoryId, destStock + cantidad);
            } else {
                createInventario(connection, idTiendaUsuario, idProducto, cantidad);
            }

            String sqlUpdateTraslado = "UPDATE TRASLADOS SET estado = 'Recibido' WHERE id_traslado = ?";
            try (PreparedStatement statement = connection.prepareStatement(sqlUpdateTraslado)) {
                statement.setInt(1, trasladoSeleccionado.getIdTraslado());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        closeWindow();
    }

    private int getInventarioId(Connection con, int idTienda, int idProd) throws SQLException {
        String sql = "SELECT id_inventario FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idTienda);
            stmt.setInt(2, idProd);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id_inventario") : -1;
            }
        }
    }

    private int getStockActual(Connection con, int idTienda, int idProd) throws SQLException {
        String sql = "SELECT stock_actual FROM INVENTARIO WHERE id_tienda = ? AND id_producto = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idTienda);
            stmt.setInt(2, idProd);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("stock_actual") : 0;
            }
        }
    }

    private void updateInventario(Connection con, int idInv, int stock) throws SQLException {
        String sql = "UPDATE INVENTARIO SET stock_actual = ?, fecha_inventario = CAST(GETDATE() AS DATE) WHERE id_inventario = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, stock);
            stmt.setInt(2, idInv);
            stmt.executeUpdate();
        }
    }

    private void createInventario(Connection con, int idTienda, int idProd, int cant) throws SQLException {
        String sql = "INSERT INTO INVENTARIO(id_tienda, id_producto, stock_actual, fecha_inventario) VALUES(?, ?, ?, CAST(GETDATE() AS DATE))";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idTienda);
            stmt.setInt(2, idProd);
            stmt.setInt(3, cant);
            stmt.executeUpdate();
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
