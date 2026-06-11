package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Inventario;
import coolbox.sistema.Modelos.MovimientoInventario;
import coolbox.sistema.Modelos.Traslado;
import coolbox.sistema.Modelos.Tienda;
import coolbox.sistema.Modelos.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class AlmacenController {

    // Componentes del Selector de Tiendas e Indicadores
    @FXML private ComboBox<Tienda> cmbFiltroTienda;
    @FXML private Label lblNombreUsuarioSidebar;
    @FXML private Label lblTiendaUsuario;
    @FXML private Button btnAltaBaja;

    // Tabla Superior: Catálogo de Productos y Stock por Tienda
    @FXML private TableView<Inventario> tblInventario;
    @FXML private TableColumn<Inventario, Integer> colIdProducto;
    @FXML private TableColumn<Inventario, String> colNombreProd;
    @FXML private TableColumn<Inventario, Integer> colStockActual;

    // Tabla de Traslados
    @FXML private TableView<Traslado> tblTraslados;
    @FXML private TableColumn<Traslado, String> colTrasladoProd;
    @FXML private TableColumn<Traslado, String> colTrasladoOrig;
    @FXML private TableColumn<Traslado, String> colTrasladoDest;
    @FXML private TableColumn<Traslado, Integer> colTrasladoCant;
    @FXML private TableColumn<Traslado, String> colTrasladoEstado; // Inyectada columna para sincronizar con FXML

    // Tabla Inferior: Kárdex de Movimientos
    @FXML private TableView<MovimientoInventario> tblMovimientos;
    @FXML private TableColumn<MovimientoInventario, String> colMovTienda;
    @FXML private TableColumn<MovimientoInventario, String> colMovTipo;
    @FXML private TableColumn<MovimientoInventario, Integer> colMovCant;
    @FXML private TableColumn<MovimientoInventario, String> colMovMotivo;
    @FXML private TableColumn<MovimientoInventario, String> colMovUsuario;
    @FXML private TableColumn<MovimientoInventario, String> colMovFecha;

    // Variables internas de estado de sesión
    private int idTiendaUsuarioActivo = -1;
    private String nombreTiendaUsuarioActivo = "Buscando...";

    @FXML
    private void initialize() {
        setupColumns();
        identificarUbicacionEmpleado();
        loadComboTiendas();
        refreshAll();
    }

    private void setupColumns() {
        colIdProducto.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombreProd.setCellValueFactory(new PropertyValueFactory<>("descripcionProducto"));
        colStockActual.setCellValueFactory(new PropertyValueFactory<>("stockActual"));

        colTrasladoProd.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colTrasladoOrig.setCellValueFactory(new PropertyValueFactory<>("origen"));
        colTrasladoDest.setCellValueFactory(new PropertyValueFactory<>("destino"));
        colTrasladoCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colTrasladoEstado.setCellValueFactory(new PropertyValueFactory<>("estado")); // Mapeo del estado de envío

        colMovTienda.setCellValueFactory(new PropertyValueFactory<>("nombreTienda")); 
        colMovTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colMovCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colMovMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMovUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario")); 
        colMovFecha.setCellValueFactory(new PropertyValueFactory<>("fechaMovimiento")); 
    }

    public void refreshAll() {
        filtrarPorTienda(); 
        loadTraslados();
        loadMovimientos();
    }

    // Identifica la tienda asignada al empleado actual cruzando la tabla de usuarios
    private void identificarUbicacionEmpleado() {
        String sql = "SELECT t.id_tienda, t.nombre_tienda FROM USUARIOS u " +
                     "JOIN EMPLEADOS e ON u.id_empleado = e.id_empleado " +
                     "JOIN TIENDAS t ON e.id_tienda = t.id_tienda " +
                     "WHERE u.nombre_usuario = ?";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SesionUsuario.getNombreUsuario());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    idTiendaUsuarioActivo = rs.getInt("id_tienda");
                    nombreTiendaUsuarioActivo = rs.getString("nombre_tienda");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        lblTiendaUsuario.setText("📍 Tu Tienda: " + nombreTiendaUsuarioActivo);
    }

    private void loadComboTiendas() {
        String sql = "SELECT id_tienda, nombre_tienda, centro_comercial FROM TIENDAS";
        ObservableList<Tienda> locales = FXCollections.observableArrayList();
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Tienda t = new Tienda();
                t.setIdTienda(rs.getInt("id_tienda"));
                t.setNombreTienda(rs.getString("nombre_tienda"));
                t.setCentroComercial(rs.getString("centro_comercial"));
                locales.add(t);

                // Preselecciona de forma automática la sucursal del operario logueado
                if (t.getIdTienda() == idTiendaUsuarioActivo) {
                    cmbFiltroTienda.setValue(t);
                }
            }
            cmbFiltroTienda.setItems(locales);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Listener automático al cambiar la selección en el Picker
    @FXML
    private void cambiarTiendaFiltro() {
        Tienda actual = cmbFiltroTienda.getValue();
        if (actual == null) return;

        // Regla de Seguridad: Habilita el botón si es su tienda, de lo contrario lo bloquea
        if (actual.getIdTienda() == idTiendaUsuarioActivo) {
            btnAltaBaja.setDisable(false);
            btnAltaBaja.setStyle("-fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btnAltaBaja.setDisable(true);
            btnAltaBaja.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: #777777; -fx-font-weight: bold; -fx-cursor: not-allowed;");
        }
        
        filtrarPorTienda();
    }

    // Acción del botón Filtrar y recarga del inventario específico de la sucursal
    @FXML
    private void filtrarPorTienda() {
        Tienda actual = cmbFiltroTienda.getValue();
        int tiendaABuscar = (actual != null) ? actual.getIdTienda() : idTiendaUsuarioActivo;

        ObservableList<Inventario> stockTienda = FXCollections.observableArrayList();
        
        // Query Multi-tienda: Trae todos los productos mapeando el stock particular de la tienda consultada
        String sql = "SELECT p.id_producto, p.nombre, ISNULL(i.stock_actual, 0) AS cantidad_tienda " +
                     "FROM PRODUCTOS p " +
                     "LEFT JOIN INVENTARIO i ON p.id_producto = i.id_producto AND i.id_tienda = ?";
        
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tiendaABuscar);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Inventario item = new Inventario();
                    item.setIdProducto(rs.getInt("id_producto"));
                    item.setDescripcionProducto(rs.getString("nombre"));
                    item.setStockActual(rs.getInt("cantidad_tienda"));
                    stockTienda.add(item);
                }
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        tblInventario.setItems(stockTienda);
    }

    private void loadMovimientos() {
        ObservableList<MovimientoInventario> movimientos = FXCollections.observableArrayList();
        String sql = "SELECT t.nombre_tienda, m.tipo_movimiento, m.cantidad, m.motivo, " +
                     "ISNULL(m.nombre_usuario, 'Sistema') AS nombre_usuario, " +
                     "ISNULL(CONVERT(VARCHAR, m.fecha, 120), 'Sin registro') AS fecha_movimiento " +
                     "FROM MOVIMIENTOS_INVENTARIO m " +
                     "JOIN TIENDAS t ON m.id_tienda = t.id_tienda";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                MovimientoInventario mov = new MovimientoInventario();
                mov.setNombreTienda(rs.getString("nombre_tienda"));
                mov.setTipoMovimiento(rs.getString("tipo_movimiento"));
                mov.setCantidad(rs.getInt("cantidad"));
                mov.setMotivo(rs.getString("motivo"));
                mov.setNombreUsuario(rs.getString("nombre_usuario"));
                mov.setFechaMovimiento(rs.getString("fecha_movimiento"));
                movimientos.add(mov);
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        tblMovimientos.setItems(movimientos);
    }

    private void loadTraslados() {
        ObservableList<Traslado> traslados = FXCollections.observableArrayList();
        // CORRECCIÓN QUERY: Se añade la selección explícita del campo 'estado' para rellenar la tabla histórica
        String sql = "SELECT t.id_traslado, p.nombre AS producto, t.id_tienda_origen AS origen, t.id_tienda_destino AS destino, t.cantidad, t.estado " +
                     "FROM TRASLADOS t LEFT JOIN PRODUCTOS p ON t.id_producto = p.id_producto";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Traslado traslado = new Traslado();
                traslado.setIdTraslado(rs.getInt("id_traslado"));
                traslado.setProducto(rs.getString("producto"));
                traslado.setOrigen(String.valueOf(rs.getInt("origen")));
                traslado.setDestino(String.valueOf(rs.getInt("destino")));
                traslado.setCantidad(rs.getInt("cantidad"));
                traslado.setEstado(rs.getString("estado")); // Sincronizado dinámicamente con el modelo
                traslados.add(traslado);
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        tblTraslados.setItems(traslados);
    }

    @FXML 
    private void abrirModalMovimiento() { 
        openModal("/coolbox/sistema/Vistas/Modales/MovimientoInventario.fxml", "Movimiento de Inventario"); 
    }

    @FXML 
    private void abrirModalTraslado() { 
        openModal("/coolbox/sistema/Vistas/Modales/RealizarTraslado.fxml", "Realizar Traslado"); 
    }

    // NUEVO MÉTODO: Mapeado al botón verde 'Recibir' de la pestaña Operaciones de tu Almacen.fxml
    @FXML 
    private void abrirModalTrasladosPendientes() { 
        openModal("/coolbox/sistema/Vistas/Modales/TrasladosPendientes.fxml", "Traslados Pendientes por Recibir"); 
    }
    
    private void openModal(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
            refreshAll(); // Refresca automáticamente las tablas al cerrar cualquier modal
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    @FXML private void irPersonal() { openModule("/coolbox/sistema/Vistas/Personal.fxml", "Coolbox - Personal"); }
    @FXML private void irOperaciones() { openModule("/coolbox/sistema/Vistas/Operaciones.fxml", "Coolbox - Operaciones"); }
    @FXML private void irReportes() { openModule("/coolbox/sistema/Vistas/Reportes.fxml", "Coolbox - Reportes"); }
    @FXML private void cerrarSesion() { openModule("/coolbox/sistema/Vistas/Login.fxml", "Coolbox - Login"); }

    @FXML 
    private void irSeguridad() { 
        if ("ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {
            openModule("/coolbox/sistema/Vistas/Seguridad.fxml", "Coolbox - Seguridad"); 
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "⛔ ACCESO RESTRINGIDO.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void openModule(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = (Stage) tblInventario.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    @FXML
    private void abrirPerfil() {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/coolbox/sistema/Vistas/Modales/PerfilUsuario.fxml"));
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Mi Perfil");
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarNombreEnSidebar() {
        if (lblNombreUsuarioSidebar != null) {
            String nombre = SesionUsuario.getNombreUsuario();
            lblNombreUsuarioSidebar.setText(nombre != null ? nombre : "");
        }
    }

}