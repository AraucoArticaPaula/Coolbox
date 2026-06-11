package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ModalRegistrarEmpleadoController implements Initializable {

    @FXML private TextField txtNombres, txtApellidos, txtDNI, txtCelular, txtCorreo, txtDireccion;
    @FXML private ComboBox<String> cmbTienda; 
    @FXML private ComboBox<String> cmbTipoEmpleado; 
    @FXML private ComboBox<String> cmbCargo; 
    @FXML private Label lblTiendaDestinoModal; 
    @FXML private Button btnCancelar;

    private int tiendaIdActual = 1;
    private final Map<String, Integer> mapaTiendas = new HashMap<>();
    
    // Lista base de cargos para restaurar al cambiar a FT
    private final ObservableList<String> cargosCompletos = FXCollections.observableArrayList("GERENTE", "SUBGERENTE", "VENDEDOR");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tiendaIdActual = SesionUsuario.getIdTiendaUsuarioConectado();
        
        cmbTipoEmpleado.getItems().addAll("FT", "PT");
        cmbCargo.setItems(cargosCompletos);

        // --- LÓGICA: RESTRICCIÓN DE CARGOS PARA PT ---
        cmbTipoEmpleado.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("PT".equals(newVal)) {
                cmbCargo.setItems(FXCollections.observableArrayList("VENDEDOR"));
                cmbCargo.getSelectionModel().select("VENDEDOR");
            } else {
                cmbCargo.setItems(cargosCompletos);
                cmbCargo.getSelectionModel().clearSelection();
            }
        });

        cargarNombresDeTiendas();
    }

    private void cargarNombresDeTiendas() {
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());
        
        String sql = esAdminGlobal ? "SELECT id_tienda, nombre_tienda FROM TIENDAS" 
                                   : "SELECT id_tienda, nombre_tienda FROM TIENDAS WHERE id_tienda = ?";
        
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            if (!esAdminGlobal) {
                statement.setInt(1, tiendaIdActual);
            }
            
            try (ResultSet rs = statement.executeQuery()) {
                cmbTienda.getItems().clear();
                mapaTiendas.clear();
                
                while (rs.next()) {
                    int id = rs.getInt("id_tienda");
                    String nombre = rs.getString("nombre_tienda").toUpperCase();
                    
                    cmbTienda.getItems().add(nombre);
                    mapaTiendas.put(nombre, id);
                }
            }
            
            // --- LÓGICA: AUTO-SELECCIÓN Y BLOQUEO DE TIENDA PARA GERENTES ---
            if (!esAdminGlobal) {
                mapaTiendas.forEach((nombre, id) -> {
                    if (id == tiendaIdActual) {
                        cmbTienda.setValue(nombre);
                    }
                });
                cmbTienda.setDisable(true); 
                lblTiendaDestinoModal.setText("📍 Registrando en sucursal: " + cmbTienda.getValue());
            } else {
                lblTiendaDestinoModal.setText("🌍 Modo Administrador: Asigne la sucursal destino.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarEmpleado() {
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String dni = txtDNI.getText().trim();
        String celular = txtCelular.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String tipoEmpleado = cmbTipoEmpleado.getValue();
        String cargoSeleccionado = cmbCargo.getValue();
        String tiendaSeleccionada = cmbTienda.getValue();

        if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || tipoEmpleado == null || cargoSeleccionado == null || tiendaSeleccionada == null) {
            mostrarAlerta("Campos obligatorios", "Por favor, complete todos los campos requeridos.", Alert.AlertType.WARNING);
            return;
        }

        int idTiendaDestino = mapaTiendas.get(tiendaSeleccionada);

        String sqlEmpleado = "INSERT INTO EMPLEADOS (id_tienda, nombres, apellidos, DNI, celular, correo, direccion, tipo_empleado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlBuscarCargo = "SELECT id_cargo FROM CARGOS WHERE UPPER(nombre_cargo) = ?";
        String sqlCargo = "INSERT INTO EMPLEADOS_CARGOS (id_empleado, id_cargo) VALUES (?, ?)";

        Connection connection = null;
        try {
            connection = ConexionDB.getConnection();
            connection.setAutoCommit(false); 

            int idCargoReal = -1;
            try (PreparedStatement psBuscarCargo = connection.prepareStatement(sqlBuscarCargo)) {
                psBuscarCargo.setString(1, cargoSeleccionado.toUpperCase());
                try (ResultSet rsCargo = psBuscarCargo.executeQuery()) {
                    if (rsCargo.next()) {
                        idCargoReal = rsCargo.getInt("id_cargo");
                    }
                }
            }

            if (idCargoReal == -1) {
                throw new SQLException("El cargo '" + cargoSeleccionado + "' no se encuentra registrado.");
            }

            int idEmpleadoGenerado = -1;
            try (PreparedStatement psEmpleado = connection.prepareStatement(sqlEmpleado, Statement.RETURN_GENERATED_KEYS)) {
                psEmpleado.setInt(1, idTiendaDestino);
                psEmpleado.setString(2, nombres);
                psEmpleado.setString(3, apellidos);
                psEmpleado.setString(4, dni);
                psEmpleado.setString(5, celular.isEmpty() ? null : celular);
                psEmpleado.setString(6, correo.isEmpty() ? null : correo);
                psEmpleado.setString(7, direccion.isEmpty() ? null : direccion);
                psEmpleado.setString(8, tipoEmpleado);

                psEmpleado.executeUpdate();

                try (ResultSet generatedKeys = psEmpleado.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        idEmpleadoGenerado = generatedKeys.getInt(1);
                    }
                }
            }

            if (idEmpleadoGenerado == -1) {
                throw new SQLException("No se pudo recuperar el identificador único.");
            }

            try (PreparedStatement psCargo = connection.prepareStatement(sqlCargo)) {
                psCargo.setInt(1, idEmpleadoGenerado);
                psCargo.setInt(2, idCargoReal);
                psCargo.executeUpdate();
            }

            connection.commit();
            mostrarAlerta("Éxito", "Empleado registrado correctamente en " + tiendaSeleccionada + ".", Alert.AlertType.INFORMATION);
            cerrarModal();

        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            mostrarAlerta("Error de Base de Datos", "No se pudo registrar: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
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