package coolbox.sistema.Controladores;

import coolbox.sistema.Conexion.ConexionDB;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkRecordarme;
    @FXML private HBox paneError;
    @FXML private Label lblErrorGlobal;

    private Preferences prefs;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        prefs = Preferences.userNodeForPackage(LoginController.class);
        String recordadoUser = prefs.get("usuario", "");
        String recordadoPass = prefs.get("password", "");

        if (!recordadoUser.isEmpty()) {
            txtUsuario.setText(recordadoUser);
            txtPassword.setText(recordadoPass);
            chkRecordarme.setSelected(true);
        }
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            paneError.setVisible(true);
            paneError.setManaged(true);
            lblErrorGlobal.setText("Por favor, ingrese sus credenciales.");
            return;
        }

        String sqlLogin = "SELECT u.id_usuario, u.nombre_usuario, e.id_empleado, e.id_tienda, c.nombre_cargo " +
                           "FROM USUARIOS u " +
                           "INNER JOIN EMPLEADOS e ON u.id_empleado = e.id_empleado " +
                           "LEFT JOIN EMPLEADOS_CARGOS ec ON e.id_empleado = ec.id_empleado " +
                           "LEFT JOIN CARGOS c ON ec.id_cargo = c.id_cargo " +
                           "WHERE u.nombre_usuario = ? AND u.contrasena = ? AND u.estado = 'activo'";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlLogin)) {
            
            statement.setString(1, usuario);
            statement.setString(2, password);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    SesionUsuario.setIdEmpleado(rs.getInt("id_empleado"));
                    SesionUsuario.setNombreUsuario(rs.getString("nombre_usuario"));
                    SesionUsuario.setIdTiendaUsuarioConectado(rs.getInt("id_tienda"));
                    
                    String cargoReal = rs.getString("nombre_cargo");
                    SesionUsuario.setRolUsuario(cargoReal != null ? cargoReal.toUpperCase() : "VENDEDOR");

                    // --- LÍNEA DE DIAGNÓSTICO ---
                    System.out.println("DEBUG: El rol cargado en la sesión es: " + SesionUsuario.getRolUsuario());

                    if (chkRecordarme.isSelected()) {
                        prefs.put("usuario", usuario);
                        prefs.put("password", password);
                    } else {
                        prefs.remove("usuario");
                        prefs.remove("password");
                    }

                    irAlPanelPrincipal(event);

                } else {
                    paneError.setVisible(true);
                    paneError.setManaged(true);
                    lblErrorGlobal.setText("Usuario o contraseña incorrectos.");
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            paneError.setVisible(true);
            paneError.setManaged(true);
            lblErrorGlobal.setText("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    private void irAlPanelPrincipal(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/coolbox/sistema/Vistas/Personal.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Coolbox - Personal");
        stage.show();
    }

    @FXML private void onOlvidePassword() {}
}