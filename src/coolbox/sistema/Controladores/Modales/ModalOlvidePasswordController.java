package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModalOlvidePasswordController {

    @FXML
    private VBox paneStep1;
    @FXML
    private VBox paneStep2;
    @FXML
    private VBox paneStep3;
    @FXML
    private VBox paneExito;

    @FXML
    private Label lblDot1;
    @FXML
    private Label lblDot2;
    @FXML
    private Label lblDot3;
    @FXML
    private Label lblStep2Lbl;
    @FXML
    private Label lblStep3Lbl;

    @FXML
    private TextField txtCorreo;
    @FXML
    private Label lblErrorStep1;

    @FXML
    private Label lblPregunta;
    @FXML
    private TextField txtRespuesta;
    @FXML
    private Label lblErrorStep2;

    @FXML
    private PasswordField txtNueva;
    @FXML
    private PasswordField txtConfirmar;
    @FXML
    private Label lblErrorStep3;

    private int idUsuarioRecuperado = -1;
    private String respuestaCorrecta = "";
    private boolean tienePregunta = false;

    @FXML
    private void onVerificarCorreo() {
        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();

        if (correo.isEmpty()) {
            mostrarError(lblErrorStep1, "Ingresa tu correo electrónico.");
            return;
        }

        String sqlUsuario = "SELECT id_usuario FROM USUARIOS WHERE correo = ? AND estado = 'activo'";

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sqlUsuario)) {

            stmt.setString(1, correo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    mostrarError(lblErrorStep1,
                            "No existe ninguna cuenta activa con ese correo.\n"
                            + "Verifica que el correo esté bien escrito.");
                    return;
                }
                idUsuarioRecuperado = rs.getInt("id_usuario");
            }

        } catch (SQLException ex) {
            mostrarError(lblErrorStep1,
                    "Error de conexión: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        String sqlPregunta
                = "SELECT pregunta, respuesta FROM PREGUNTAS_SEGURIDAD WHERE id_usuario = ?";

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sqlPregunta)) {

            stmt.setInt(1, idUsuarioRecuperado);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tienePregunta = true;
                    respuestaCorrecta = rs.getString("respuesta").toLowerCase().trim();
                    lblPregunta.setText(rs.getString("pregunta"));
                } else {

                    tienePregunta = false;
                    lblPregunta.setText(
                            "⚠ Tu cuenta no tiene una pregunta de seguridad configurada.\n"
                            + "Contacta al administrador para restablecer tu contraseña.");
                    txtRespuesta.setDisable(true);
                    txtRespuesta.setPromptText("No disponible");
                }
            }

        } catch (SQLException ex) {

            tienePregunta = false;
            lblPregunta.setText(
                    "⚠ El sistema de preguntas de seguridad no está configurado.\n"
                    + "Contacta al administrador.");
            txtRespuesta.setDisable(true);
            System.err.println("PREGUNTAS_SEGURIDAD no disponible: " + ex.getMessage());
        }

        ocultarError(lblErrorStep1);
        irAPaso(2);
    }

    @FXML
    private void onVerificarRespuesta() {

        if (!tienePregunta) {
            mostrarError(lblErrorStep2,
                    "No puedes continuar sin una pregunta de seguridad.\nContacta al administrador.");
            return;
        }

        String respuesta = txtRespuesta.getText() == null ? "" : txtRespuesta.getText().trim();

        if (respuesta.isEmpty()) {
            mostrarError(lblErrorStep2, "Ingresa tu respuesta.");
            return;
        }

        if (respuesta.toLowerCase().equals(respuestaCorrecta)) {
            ocultarError(lblErrorStep2);
            txtRespuesta.setDisable(false);
            irAPaso(3);
        } else {
            mostrarError(lblErrorStep2, "Respuesta incorrecta. Inténtalo de nuevo.");
            txtRespuesta.clear();
            txtRespuesta.requestFocus();
        }
    }

    @FXML
    private void onCambiarContrasena() {
        String nueva = txtNueva.getText();
        String confirmar = txtConfirmar.getText();

        if (nueva == null || nueva.length() < 6) {
            mostrarError(lblErrorStep3, "La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!nueva.equals(confirmar)) {
            mostrarError(lblErrorStep3, "Las contraseñas no coinciden.");
            txtConfirmar.clear();
            txtConfirmar.requestFocus();
            return;
        }

        String sql = "UPDATE USUARIOS SET contrasena = ? WHERE id_usuario = ?";

        try (Connection con = ConexionDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, nueva);
            stmt.setInt(2, idUsuarioRecuperado);

            if (stmt.executeUpdate() > 0) {
                ocultarError(lblErrorStep3);
                irAPaso(4);
            } else {
                mostrarError(lblErrorStep3, "No se pudo actualizar. Inténtalo de nuevo.");
            }

        } catch (SQLException ex) {
            mostrarError(lblErrorStep3, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onVolverStep1() {
        txtRespuesta.setDisable(false);
        txtRespuesta.clear();
        irAPaso(1);
    }

    @FXML
    private void onVolverStep2() {
        irAPaso(2);
    }

    @FXML
    private void onCerrar() {
        ((Stage) paneStep1.getScene().getWindow()).close();
    }

    private void irAPaso(int paso) {
        setPane(paneStep1, false);
        setPane(paneStep2, false);
        setPane(paneStep3, false);
        setPane(paneExito, false);

        switch (paso) {
            case 1 -> {
                setPane(paneStep1, true);
                txtCorreo.requestFocus();
            }
            case 2 -> {
                setPane(paneStep2, true);
                if (tienePregunta) {
                    txtRespuesta.requestFocus();
            
                }}
            case 3 -> {
                setPane(paneStep3, true);
                txtNueva.requestFocus();
            }
            case 4 ->
                setPane(paneExito, true);
        }
        actualizarIndicadores(paso);
    }

    private void setPane(VBox pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void actualizarIndicadores(int paso) {
        String activo = "-fx-text-fill: #CC0000; -fx-font-size: 20px;";
        String hecho = "-fx-text-fill: #22a94f; -fx-font-size: 20px;";
        String pend = "-fx-text-fill: #aaa;    -fx-font-size: 20px;";
        String lActivo = "-fx-font-size: 11px; -fx-text-fill: #333; -fx-font-weight: bold;";
        String lPend = "-fx-font-size: 11px; -fx-text-fill: #aaa;";

        if (paso == 1) {
            lblDot1.setText("●");
            lblDot1.setStyle(activo);
            lblDot2.setText("○");
            lblDot2.setStyle(pend);
            lblDot3.setText("○");
            lblDot3.setStyle(pend);
            lblStep2Lbl.setStyle(lPend);
            lblStep3Lbl.setStyle(lPend);
        } else if (paso == 2) {
            lblDot1.setText("✔");
            lblDot1.setStyle(hecho);
            lblDot2.setText("●");
            lblDot2.setStyle(activo);
            lblDot3.setText("○");
            lblDot3.setStyle(pend);
            lblStep2Lbl.setStyle(lActivo);
            lblStep3Lbl.setStyle(lPend);
        } else {
            lblDot1.setText("✔");
            lblDot1.setStyle(hecho);
            lblDot2.setText("✔");
            lblDot2.setStyle(hecho);
            lblDot3.setText(paso == 4 ? "✔" : "●");
            lblDot3.setStyle(paso == 4 ? hecho : activo);
            lblStep2Lbl.setStyle(lActivo);
            lblStep3Lbl.setStyle(lActivo);
        }
    }

    private void mostrarError(Label lbl, String mensaje) {
        lbl.setText(mensaje);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void ocultarError(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }
}
