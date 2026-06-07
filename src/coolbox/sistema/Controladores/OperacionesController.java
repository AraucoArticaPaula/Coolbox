package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Cuadre;
import coolbox.sistema.Modelos.Venta;
import coolbox.sistema.Controladores.SesionUsuario; // Asegurado el paquete exacto de tu sesión
import coolbox.sistema.Controladores.Modales.ModalRegistrarCuadreController; // Import para capturar el callback
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperacionesController {

    // COMPONENTES DE FILTRADO POR PERIODO DE TIEMPO
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, Integer> colIdVenta;
    @FXML private TableColumn<Venta, String> colNumeroBoleta;   
    @FXML private TableColumn<Venta, Integer> colTiendaVenta;  
    @FXML private TableColumn<Venta, Integer> colEmpleadoVenta; 
    @FXML private TableColumn<Venta, String> colFechaVenta;
    @FXML private TableColumn<Venta, Double> colMontoVenta;
    @FXML private TableColumn<Venta, Integer> colProductosVenta;

    @FXML private TableView<Cuadre> tblCuadres;
    @FXML private TableColumn<Cuadre, Integer> colIdCuadre;
    @FXML private TableColumn<Cuadre, Integer> colTiendaCuadre;  
    @FXML private TableColumn<Cuadre, Integer> colEmpleadoCuadre; 
    @FXML private TableColumn<Cuadre, String> colFechaCuadre;
    @FXML private TableColumn<Cuadre, Double> colMontoCuadre;
    @FXML private TableColumn<Cuadre, String> colObsCuadre;

    private final Map<Integer, String> mapaTiendas = new HashMap<>();
    private final Map<Integer, String> mapaEmpleados = new HashMap<>();

    @FXML
    private void initialize() {
        precargarDiccionariosNombres(); 
        setupColumns();
        configurarRenderizadoDeNombres(); 
        refreshAll();
    }

    private void precargarDiccionariosNombres() {
        mapaTiendas.clear();
        mapaEmpleados.clear();

        String sqlTiendas = "SELECT id_tienda, nombre_tienda FROM TIENDAS";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlTiendas);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mapaTiendas.put(rs.getInt("id_tienda"), rs.getString("nombre_tienda"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String sqlEmpleados = "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlEmpleados);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mapaEmpleados.put(rs.getInt("id_empleado"), rs.getString("nombres") + " " + rs.getString("apellidos"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupColumns() {
        colIdVenta.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        colNumeroBoleta.setCellValueFactory(new PropertyValueFactory<>("numeroBoleta")); 
        colTiendaVenta.setCellValueFactory(new PropertyValueFactory<>("idTienda"));
        colEmpleadoVenta.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colFechaVenta.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colMontoVenta.setCellValueFactory(new PropertyValueFactory<>("montoTotal"));
        colProductosVenta.setCellValueFactory(new PropertyValueFactory<>("cantidadProductos"));

        colIdCuadre.setCellValueFactory(new PropertyValueFactory<>("idCuadre"));
        colTiendaCuadre.setCellValueFactory(new PropertyValueFactory<>("idTienda"));
        colEmpleadoCuadre.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colFechaCuadre.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colMontoCuadre.setCellValueFactory(new PropertyValueFactory<>("montoTotal"));
        colObsCuadre.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
    }

    private void configurarRenderizadoDeNombres() {
        var celdaTiendaFactory = new javafx.util.Callback<TableColumn<Venta, Integer>, TableCell<Venta, Integer>>() {
            @Override public TableCell<Venta, Integer> call(TableColumn<Venta, Integer> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(Integer id, boolean empty) {
                        super.updateItem(id, empty);
                        if (empty || id == null) { setText(null); } 
                        else { setText(mapaTiendas.getOrDefault(id, "Sede #" + id)); }
                    }
                };
            }
        };
        colTiendaVenta.setCellFactory(celdaTiendaFactory);
        
        colTiendaCuadre.setCellFactory(new javafx.util.Callback<>() {
            @Override public TableCell<Cuadre, Integer> call(TableColumn<Cuadre, Integer> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(Integer id, boolean empty) {
                        super.updateItem(id, empty);
                        if (empty || id == null) { setText(null); } 
                        else { setText(mapaTiendas.getOrDefault(id, "Sede #" + id)); }
                    }
                };
            }
        });

        var celdaEmpleadoFactory = new javafx.util.Callback<TableColumn<Venta, Integer>, TableCell<Venta, Integer>>() {
            @Override public TableCell<Venta, Integer> call(TableColumn<Venta, Integer> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(Integer id, boolean empty) {
                        super.updateItem(id, empty);
                        if (empty || id == null) { setText(null); } 
                        else { setText(mapaEmpleados.getOrDefault(id, "Vendedor #" + id)); }
                    }
                };
            }
        };
        colEmpleadoVenta.setCellFactory(celdaEmpleadoFactory);

        colEmpleadoCuadre.setCellFactory(new javafx.util.Callback<>() {
            @Override public TableCell<Cuadre, Integer> call(TableColumn<Cuadre, Integer> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(Integer id, boolean empty) {
                        super.updateItem(id, empty);
                        if (empty || id == null) { setText(null); } 
                        else { setText(mapaEmpleados.getOrDefault(id, "Vendedor #" + id)); }
                    }
                };
            }
        });
    }

    private void refreshAll() {
        precargarDiccionariosNombres(); 
        loadVentas();
        loadCuadres();
    }

    @FXML 
    private void abrirModalRegistrarVenta() { 
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coolbox/sistema/Vistas/Modales/RegistrarVenta.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Registrar Venta");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            
            refreshAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML 
    private void abrirModalRegistrarCuadre() { 
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coolbox/sistema/Vistas/Modales/RegistrarCuadre.fxml"));
            Parent root = loader.load();
            
            // Capturamos el controlador del modal de forma segura
            ModalRegistrarCuadreController controller = loader.getController();
            
            // Asignamos la acción para que refresque las grillas automáticamente al guardar
            controller.setOnGuardarSuccess(this::refreshAll);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Registrar Cuadre");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("❌ Error al instanciar el modal relacional de arqueos.");
            e.printStackTrace();
        }
    }

    @FXML
    private void filtrarVentasPorFecha() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();

        if (inicio == null || fin == null) {
            loadVentas();
            return;
        }

        List<Venta> ventasFiltradas = new ArrayList<>();
        String sql = "SELECT id_venta, id_tienda, id_empleado, CONVERT(varchar, fecha, 120) AS fecha, " +
                     "monto AS monto_total, productos_por_venta AS cantidad_productos, numero_boleta " +
                     "FROM VENTAS WHERE CONVERT(date, fecha) BETWEEN ? AND ? ORDER BY id_venta DESC";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, java.sql.Date.valueOf(inicio));
            statement.setDate(2, java.sql.Date.valueOf(fin));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Venta venta = new Venta();
                    venta.setIdVenta(rs.getInt("id_venta"));
                    venta.setIdTienda(rs.getInt("id_tienda"));
                    venta.setIdEmpleado(rs.getInt("id_empleado"));
                    venta.setFecha(rs.getString("fecha"));
                    venta.setMontoTotal(rs.getDouble("monto_total"));
                    venta.setCantidadProductos(rs.getInt("cantidad_productos"));
                    
                    String numBoleta = rs.getString("numero_boleta");
                    venta.setNumeroBoleta(numBoleta != null ? numBoleta : "S/N");
                    
                    ventasFiltradas.add(venta);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error crítico en el filtrado por rango.");
            e.printStackTrace();
        }
        tblVentas.setItems(FXCollections.observableArrayList(ventasFiltradas));
    }

    private void loadVentas() {
        List<Venta> ventas = new ArrayList<>();
        String sql = "SELECT id_venta, id_tienda, id_empleado, CONVERT(varchar, fecha, 120) AS fecha, " +
                     "monto AS monto_total, productos_por_venta AS cantidad_productos, numero_boleta FROM VENTAS ORDER BY id_venta DESC";
        
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Venta venta = new Venta();
                venta.setIdVenta(rs.getInt("id_venta"));
                venta.setIdTienda(rs.getInt("id_tienda"));
                venta.setIdEmpleado(rs.getInt("id_empleado"));
                venta.setFecha(rs.getString("fecha"));
                venta.setMontoTotal(rs.getDouble("monto_total"));
                venta.setCantidadProductos(rs.getInt("cantidad_productos"));
                
                String numBoleta = rs.getString("numero_boleta");
                venta.setNumeroBoleta(numBoleta != null ? numBoleta : "S/N");
                
                ventas.add(venta);
            }
        } catch (SQLException e) { 
            System.err.println("❌ Error en mapeo de la tabla VENTAS.");
            e.printStackTrace(); 
        }
        tblVentas.setItems(FXCollections.observableArrayList(ventas));
    }

    private void loadCuadres() {
        List<Cuadre> cuadres = new ArrayList<>();
        // 🌟 CORREGIDO: Apuntando al nombre físico exacto de tu tabla relacional 'CUADRE_CAJA'
        String sql = "SELECT id_cuadre, id_tienda, id_empleado, CONVERT(varchar, fecha, 120) AS fecha, monto_total, observaciones FROM CUADRE_CAJA ORDER BY id_cuadre DESC";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Cuadre cuadre = new Cuadre();
                cuadre.setIdCuadre(rs.getInt("id_cuadre"));
                cuadre.setIdTienda(rs.getInt("id_tienda"));
                cuadre.setIdEmpleado(rs.getInt("id_empleado"));
                cuadre.setFecha(rs.getString("fecha"));
                cuadre.setMontoTotal(rs.getDouble("monto_total"));
                cuadre.setObservaciones(rs.getString("observaciones"));
                cuadres.add(cuadre);
            }
        } catch (SQLException e) { 
            System.err.println("❌ Error al leer la tabla relacional CUADRE_CAJA.");
            e.printStackTrace(); 
        }
        tblCuadres.setItems(FXCollections.observableArrayList(cuadres));
    }

    @FXML private void irPersonal() { openModule("/coolbox/sistema/Vistas/Personal.fxml", "Coolbox - Personal"); }
    @FXML private void irAlmacen() { openModule("/coolbox/sistema/Vistas/Almacen.fxml", "Coolbox - Almacén"); }
    @FXML private void irReportes() { openModule("/coolbox/sistema/Vistas/Reportes.fxml", "Coolbox - Reportes"); }
    @FXML private void cerrarSesion() { openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login"); }

    @FXML 
    private void irSeguridad() { 
        String rolActual = "INVITADO";
        try {
            if (SesionUsuario.getRolUsuario() != null) {
                rolActual = SesionUsuario.getRolUsuario();
            }
        } catch (Throwable t) {
            System.err.println("No se pudo obtener el rol estático.");
        }

        if ("ADMINISTRADOR".equalsIgnoreCase(rolActual)) {
            openModule("/coolbox/sistema/Vistas/Seguridad.fxml", "Coolbox - Seguridad"); 
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "⛔ ACCESO RESTRINGIDO\n\nEl módulo de Seguridad es exclusivo para Administradores.", ButtonType.OK);
            alert.setTitle("Restricción de Privilegios");
            alert.showAndWait();
        }
    }

    private void openModule(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = (Stage) tblVentas.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            System.err.println("❌ ERROR AL CARGAR MÓDULO: " + title);
            e.printStackTrace();
        }
    }
}