package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ModalAsignarCargoController implements Initializable {

    @FXML private ComboBox<String> cmbEmpleado; // Desplegará "ID - Nombre Apellido"
    @FXML private ComboBox<String> cmbCargo;    // Desplegará los cargos reales de la BD ('GERENTE', etc.)
    @FXML private Button btnCancelar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarEmpleadosDesdeBD();
        cargarCargosDesdeBD();
    }

    // Solución a: "no despliega la lista de empleados"
    private void cargarEmpleadosDesdeBD() {
        String sql = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            cmbEmpleado.getItems().clear();
            while (rs.next()) {
                int id = rs.getInt("id_empleado");
                String nombreCompleto = rs.getString("nombres") + " " + rs.getString("apellidos");
                // Guardamos en formato "ID - Nombre" para poder separar el ID fácilmente al guardar
                cmbEmpleado.getItems().add(id + " - " + nombreCompleto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Solución a: "salen más de los que deberían y salen en minúsculas"
    private void cargarCargosDesdeBD() {
        String sql = "SELECT nombre_cargo FROM CARGOS";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            cmbCargo.getItems().clear();
            while (rs.next()) {
                // Trae los cargos tal cual están en tu BD (ej: GERENTE, SUBGERENTE, VENDEDOR)
                cmbCargo.getItems().add(rs.getString("nombre_cargo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Si por alguna razón tu tabla CARGOS está vacía, ponemos los permitidos por tu CHECK en mayúsculas
        if (cmbCargo.getItems().isEmpty()) {
            cmbCargo.getItems().addAll("GERENTE", "SUBGERENTE", "VENDEDOR");
        }
    }

    @FXML
    private void guardarAsignacion() {
        String empleadoSeleccionado = cmbEmpleado.getValue();
        String cargoSeleccionado = cmbCargo.getValue();

        if (empleadoSeleccionado == null || cargoSeleccionado == null) {
            mostrarAlerta("Campos requeridos", "Por favor, seleccione un colaborador y el nuevo cargo.", Alert.AlertType.WARNING);
            return;
        }

        // Extraemos el ID numérico del empleado (lo que está antes del guion " - ")
        int idEmpleado = Integer.parseInt(empleadoSeleccionado.split(" - ")[0]);

        // Sentencias SQL para el ascenso / cambio de cargo en la tabla intermedia EMPLEADOS_CARGOS
        String sqlBuscarCargo = "SELECT id_cargo FROM CARGOS WHERE nombre_cargo = ?";
        String sqlActualizarCargo = "UPDATE EMPLEADOS_CARGOS SET id_cargo = ? WHERE id_empleado = ?";
        String sqlInsertarCargo = "INSERT INTO EMPLEADOS_CARGOS (id_empleado, id_cargo) VALUES (?, ?)";

        try (Connection connection = ConexionDB.getConnection()) {
            // 1. Obtener el id_cargo real de la base de datos
            int idCargoReal = -1;
            try (PreparedStatement psBuscar = connection.prepareStatement(sqlBuscarCargo)) {
                psBuscar.setString(1, cargoSeleccionado);
                try (ResultSet rs = psBuscar.executeQuery()) {
                    if (rs.next()) {
                        idCargoReal = rs.getInt("id_cargo");
                    }
                }
            }

            // 2. Intentamos actualizar el cargo existente (Ascenso). Si no tiene registro previo, lo insertamos.
            int filasActualizadas = 0;
            try (PreparedStatement psUpdate = connection.prepareStatement(sqlActualizarCargo)) {
                psUpdate.setInt(1, idCargoReal);
                psUpdate.setInt(2, idEmpleado);
                filasActualizadas = psUpdate.executeUpdate();
            }

            if (filasActualizadas == 0) {
                // Si el empleado no tenía un cargo asignado en la tabla intermedia, lo creamos
                try (PreparedStatement psInsert = connection.prepareStatement(sqlInsertarCargo)) {
                    psInsert.setInt(1, idEmpleado);
                    psInsert.setInt(2, idCargoReal);
                    psInsert.executeUpdate();
                }
            }

            mostrarAlerta("Éxito", "El cargo ha sido asignado/actualizado correctamente en la base de datos.", Alert.AlertType.INFORMATION);
            cerrarModal();

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error de Base de Datos", "No se pudo procesar la asignación: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}