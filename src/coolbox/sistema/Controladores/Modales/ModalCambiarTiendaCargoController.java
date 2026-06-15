package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Empleado;
import coolbox.sistema.Modelos.Tienda;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalCambiarTiendaCargoController {

    @FXML
    private Label lblEmpleado;
    @FXML
    private ComboBox<Tienda> cmbTienda;
    @FXML
    private ComboBox<String> cmbCargo;
    @FXML
    private ComboBox<String> cmbModalidad;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    private Empleado empleadoActual;

    @FXML
    private void initialize() {
        loadTiendas();

        cmbCargo.setItems(FXCollections.observableArrayList("ADMINISTRADOR", "GERENTE", "SUBGERENTE", "VENDEDOR"));
        cmbModalidad.setItems(FXCollections.observableArrayList("FT", "PT"));
        cmbCargo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("ADMINISTRADOR") || newValue.equals("GERENTE") || newValue.equals("SUBGERENTE")) {
                    cmbModalidad.setValue("FT");
                    cmbModalidad.setDisable(true);
                } else {
                    cmbModalidad.setDisable(false);
                }
            }
        });
    }

    public void initData(Empleado empleado) {
        this.empleadoActual = empleado;
        lblEmpleado.setText("Empleado: " + empleado.getNombres() + " " + empleado.getApellidos());

        if (empleado.getTipo() != null) {
            cmbModalidad.setValue(empleado.getTipo().toUpperCase());
        }

        if (cmbTienda.getItems() != null) {
            for (Tienda t : cmbTienda.getItems()) {
                if (t.getIdTienda() == empleado.getIdTienda()) {
                    cmbTienda.setValue(t);
                    break;
                }
            }
        }

        String sqlCargo = "SELECT C.nombre_cargo FROM CARGOS C "
                + "JOIN EMPLEADOS_CARGOS EC ON C.id_cargo = EC.id_cargo "
                + "WHERE EC.id_empleado = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement stmt = conn.prepareStatement(sqlCargo)) {
            stmt.setInt(1, empleado.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cmbCargo.setValue(rs.getString("nombre_cargo"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTiendas() {
        String sql = "SELECT id_tienda, nombre_tienda, centro_comercial FROM TIENDAS";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {

            ObservableList<Tienda> tiendas = FXCollections.observableArrayList();
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

    @FXML
    private void guardarCambios() {
        Tienda tiendaSeleccionada = cmbTienda.getValue();
        String cargoSeleccionado = cmbCargo.getValue();
        String modalidadSeleccionada = cmbModalidad.getValue();

        if (empleadoActual == null || tiendaSeleccionada == null || cargoSeleccionado == null || modalidadSeleccionada == null) {
            mostrarAlerta("❌ Por favor, complete todos los campos.");
            return;
        }

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            String sql1 = "UPDATE EMPLEADOS SET id_tienda = ?, tipo_empleado = ? WHERE id_empleado = ?";
            try (PreparedStatement stmt1 = conn.prepareStatement(sql1)) {
                stmt1.setInt(1, tiendaSeleccionada.getIdTienda());
                stmt1.setString(2, modalidadSeleccionada);
                stmt1.setInt(3, empleadoActual.getId());
                stmt1.executeUpdate();
            }

            String sql2 = "DELETE FROM EMPLEADOS_CARGOS WHERE id_empleado = ?";
            try (PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
                stmt2.setInt(1, empleadoActual.getId());
                stmt2.executeUpdate();
            }

            String sql3 = "INSERT INTO EMPLEADOS_CARGOS (id_empleado, id_cargo) "
                    + "SELECT ?, id_cargo FROM CARGOS WHERE nombre_cargo = ?";
            try (PreparedStatement stmt3 = conn.prepareStatement(sql3)) {
                stmt3.setInt(1, empleadoActual.getId());
                stmt3.setString(2, cargoSeleccionado);
                stmt3.executeUpdate();
            }

            int idUsuario = -1;
            String sqlVerificarUsuario = "SELECT id_usuario FROM USUARIOS WHERE id_empleado = ?";
            try (PreparedStatement stmtVU = conn.prepareStatement(sqlVerificarUsuario)) {
                stmtVU.setInt(1, empleadoActual.getId());
                try (ResultSet rs = stmtVU.executeQuery()) {
                    if (rs.next()) {
                        idUsuario = rs.getInt("id_usuario");
                    }
                }
            }

            if (idUsuario != -1) {
                String sqlDelRol = "DELETE FROM USUARIOS_ROLES WHERE id_usuario = ?";
                try (PreparedStatement stmtDR = conn.prepareStatement(sqlDelRol)) {
                    stmtDR.setInt(1, idUsuario);
                    stmtDR.executeUpdate();
                }

                String sqlInsRol = "INSERT INTO USUARIOS_ROLES (id_usuario, id_rol) "
                        + "SELECT ?, id_rol FROM ROLES WHERE nombre_rol = ?";
                try (PreparedStatement stmtIR = conn.prepareStatement(sqlInsRol)) {
                    stmtIR.setInt(1, idUsuario);
                    stmtIR.setString(2, cargoSeleccionado);
                    stmtIR.executeUpdate();
                }
            }

            conn.commit();
            cerrarModal();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            mostrarAlerta("Error al actualizar la base de datos.");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void cerrarModal() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
