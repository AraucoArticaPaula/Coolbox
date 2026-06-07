package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Modelos.Traslado;
import coolbox.sistema.Controladores.SesionUsuario; // 👈 IMPORTACIÓN OBLIGATORIA AGREGADA
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

public class ModalTrasladosPendientesController {

    @FXML private TableView<Traslado> tblPendientes;
    @FXML private TableColumn<Traslado, String> colProd;
    @FXML private TableColumn<Traslado, String> colOrig;
    @FXML private TableColumn<Traslado, Integer> colCant;
    @FXML private Button btnCerrar;

    private int idTiendaUsuario = -1;

    @FXML
    private void initialize() {
        colProd.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colOrig.setCellValueFactory(new PropertyValueFactory<>("origen"));
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        obtenerTiendaUsuario();
        loadPendientes();
    }

    private void obtenerTiendaUsuario() {
        String sql = "SELECT id_tienda FROM EMPLEADOS e JOIN USUARIOS u ON e.id_empleado = u.id_empleado WHERE u.nombre_usuario = ?";
        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, SesionUsuario.getNombreUsuario());
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) idTiendaUsuario = rs.getInt("id_tienda"); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadPendientes() {
        ObservableList<Traslado> pendientes = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder(
            "SELECT t.id_traslado, p.nombre AS producto, ti.nombre_tienda AS origen, t.id_tienda_destino, t.cantidad, t.estado " +
            "FROM TRASLADOS t " +
            "JOIN PRODUCTOS p ON t.id_producto = p.id_producto " +
            "JOIN TIENDAS ti ON t.id_tienda_origen = ti.id_tienda " +
            "WHERE t.estado = 'Pendiente'"
        );

        if (!"ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario())) {
            sql.append(" AND t.id_tienda_destino = ").append(idTiendaUsuario);
        }

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql.toString()); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Traslado t = new Traslado();
                t.setIdTraslado(rs.getInt("id_traslado"));
                t.setProducto(rs.getString("producto"));
                t.setOrigen(rs.getString("origen"));
                t.setDestino(String.valueOf(rs.getInt("id_tienda_destino")));
                t.setCantidad(rs.getInt("cantidad"));
                t.setEstado(rs.getString("estado"));
                pendientes.add(t);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        tblPendientes.setItems(pendientes);
    }

    @FXML
    private void procesarSeleccionado() {
        Traslado t = tblPendientes.getSelectionModel().getSelectedItem();
        if (t == null) return;

        try {
            ModalRecibirTrasladoController.setTrasladoAProcesar(t);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coolbox/sistema/Vistas/Modales/RecibirTraslado.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Confirmar Ingreso de Stock");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            loadPendientes(); 
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void cerrar() { ((Stage) btnCerrar.getScene().getWindow()).close(); }
}