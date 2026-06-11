package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter; // IMPORTACIÓN ASEGURADA PARA EVITAR EL FALLO DE APERTURA
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
import java.util.ResourceBundle;

public class ModalValidarCumplimientoController implements Initializable {

    @FXML private ComboBox<String> cmbEmpleado;
    @FXML private DatePicker dpFechaSemana;
    @FXML private TextField txtPeriodo;
    
    @FXML private TextField txtHorasCumplidas;
    @FXML private TextField txtMinutosCumplidos;
    
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private int idEmpleadoSeleccionado = -1;
    private String tipoEmpleadoContrato = "PT";
    private double totalHorasDecimalFinal = 0.0;
    private String stringRangoPeriodo = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarEmpleadosDesdeBD();
        aplicarFiltrosDeSeguridadNumerica();

        // Listener en la propiedad value para que dispare siempre,
        // tanto al seleccionar del calendario como al escribir la fecha
        dpFechaSemana.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) onFechaSeleccionada();
        });
    }

    private void cargarEmpleadosDesdeBD() {
        int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());

        String sql = esAdminGlobal ? "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS"
                                   : "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_tienda = ?";
                                   
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!esAdminGlobal) {
                statement.setInt(1, idTiendaSesion);
            }
            try (ResultSet rs = statement.executeQuery()) {
                cmbEmpleado.getItems().clear();
                while (rs.next()) {
                    cmbEmpleado.getItems().add(rs.getInt("id_empleado") + " - " + rs.getString("nombres") + " " + rs.getString("apellidos"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void aplicarFiltrosDeSeguridadNumerica() {
        // Bloquea por completo el teclado si intentan meter letras o caracteres raros
        txtHorasCumplidas.setTextFormatter(new TextFormatter<>(change -> 
            change.getText().matches("\\d*") ? change : null));
            
        txtMinutosCumplidos.setTextFormatter(new TextFormatter<>(change -> 
            change.getText().matches("\\d*") ? change : null));
    }

    @FXML
    private void onEmpleadoSeleccionado() {
        String seleccionado = cmbEmpleado.getValue();
        if (seleccionado == null) return;

        idEmpleadoSeleccionado = Integer.parseInt(seleccionado.split(" - ")[0]);
        
        String sql = "SELECT tipo_empleado FROM EMPLEADOS WHERE id_empleado = ?";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEmpleadoSeleccionado);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    tipoEmpleadoContrato = rs.getString("tipo_empleado");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        evaluarTiempoIngresado();
    }

    @FXML
    private void onFechaSeleccionada() {
        LocalDate fechaElegida = dpFechaSemana.getValue();
        if (fechaElegida == null) return;

        LocalDate lunesDeEsaSemana = fechaElegida.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingoDeEsaSemana = fechaElegida.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        DateTimeFormatter formatoVisual = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        stringRangoPeriodo = lunesDeEsaSemana.format(formatoVisual) + " al " + domingoDeEsaSemana.format(formatoVisual);
        txtPeriodo.setText("Semana: " + stringRangoPeriodo);

        evaluarTiempoIngresado();
    }

    @FXML
    private void evaluarTiempoIngresado() {
        String strHoras = txtHorasCumplidas.getText().trim();
        String strMinutos = txtMinutosCumplidos.getText().trim();

        if (idEmpleadoSeleccionado == -1 || stringRangoPeriodo.isEmpty() || strHoras.isEmpty() || strMinutos.isEmpty()) {
            btnGuardar.setDisable(true);
            return;
        }

        try {
            int horas = Integer.parseInt(strHoras);
            int minutes = Integer.parseInt(strMinutos);

            if (minutes >= 60) {
                btnGuardar.setDisable(true);
                return;
            }

            double fraccionMinutos = (double) minutes / 60.0;
            totalHorasDecimalFinal = horas + fraccionMinutos;

            btnGuardar.setDisable(false);
            
        } catch (NumberFormatException e) {
            btnGuardar.setDisable(true);
        }
    }

    @FXML
    private void guardarValidacion() {
        if (idEmpleadoSeleccionado == -1 || stringRangoPeriodo.isEmpty()) return;

        String estadoValidacion = "cumplido";
        if ("FT".equalsIgnoreCase(tipoEmpleadoContrato) && totalHorasDecimalFinal < 48.0) {
            estadoValidacion = "incumplido"; 
        } else if ("PT".equalsIgnoreCase(tipoEmpleadoContrato) && totalHorasDecimalFinal < 24.0) {
            estadoValidacion = "incumplido"; 
        }

        String sqlInsert = "INSERT INTO CUMPLIMIENTO_HORAS (id_empleado, horas_semana, tipo_empleado, estado_validacion, periodo) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlInsert)) {
            
            statement.setInt(1, idEmpleadoSeleccionado);
            statement.setDouble(2, totalHorasDecimalFinal); 
            statement.setString(3, tipoEmpleadoContrato);
            statement.setString(4, estadoValidacion); 
            statement.setString(5, stringRangoPeriodo);

            int filas = statement.executeUpdate();
            if (filas > 0) {
                mostrarAlerta("Auditoría Registrada", "Se auditó un total de " + totalHorasDecimalFinal + " hrs. Resultado: [" + estadoValidacion.toUpperCase() + "]. Guardado en base de datos.", Alert.AlertType.INFORMATION);
                cerrarModal();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo registrar la validación: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void cerrarModal() { ((Stage) btnCancelar.getScene().getWindow()).close(); }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}