package coolbox.sistema.Controladores.Modales;

import coolbox.sistema.Conexion.ConexionDB;
import coolbox.sistema.Controladores.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ModalRegistrarHorarioController implements Initializable {

    @FXML
    private ComboBox<String> cmbEmpleado;
    @FXML
    private Label lblJornadaInfo;
    @FXML
    private Label lblContadorHoras;
    @FXML
    private Label lblTiendaCabecera;
    @FXML
    private ProgressBar progressHoras;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    @FXML
    private ComboBox<String> cmbPickerDia;
    @FXML
    private ComboBox<String> cmbPickerInicio;
    @FXML
    private ComboBox<String> cmbPickerFin;

    @FXML
    private DatePicker dpFechaInicio;
    @FXML
    private DatePicker dpFechaFin;

    @FXML
    private TableView<FilaHorarioGrafico> tblMatrizHorario;
    @FXML
    private TableColumn<FilaHorarioGrafico, String> colTramo;
    @FXML
    private TableColumn<FilaHorarioGrafico, List<AsignacionItem>> colLunes, colMartes, colMiercoles, colJueves, colViernes, colSabado, colDomingo;

    private int horasObjetivo = 0;
    private int horasAsignadasActuales = 0;
    private String esFullTimeOPT = "PT";
    private String nombreTiendaActual = "Sucursal Activa";

    private final String[] paletaColores = {
        "-fx-background-color: #20c997; -fx-text-fill: white;",
        "-fx-background-color: #0d6efd; -fx-text-fill: white;",
        "-fx-background-color: #6f42c1; -fx-text-fill: white;",
        "-fx-background-color: #d63384; -fx-text-fill: white;",
        "-fx-background-color: #fd7e14; -fx-text-fill: white;",
        "-fx-background-color: #198754; -fx-text-fill: white;"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        obtenerYMostrarNombreTienda();
        cargarEmpleadosDesdeBD();
        inicializarPickersRangos();
        configurarTablaGrafica();
        configurarFechasAutomaticas();
    }

    private void configurarFechasAutomaticas() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunesProximo = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate domingoProximo = lunesProximo.plusDays(6);

        dpFechaInicio.setValue(lunesProximo);
        dpFechaFin.setValue(domingoProximo);

        dpFechaInicio.setDisable(true);
        dpFechaFin.setDisable(true);
    }

    private void obtenerYMostrarNombreTienda() {
        int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
        String sql = "SELECT nombre_tienda FROM TIENDAS WHERE id_tienda = ?";

        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idTiendaSesion);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    nombreTiendaActual = rs.getString("nombre_tienda");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (lblTiendaCabecera != null) {
            lblTiendaCabecera.setText("🏪 TIENDA: " + nombreTiendaActual.toUpperCase());
        }
    }

    private void cargarEmpleadosDesdeBD() {
        int idTiendaSesion = SesionUsuario.getIdTiendaUsuarioConectado();
        boolean esAdminGlobal = "ADMINISTRADOR".equalsIgnoreCase(SesionUsuario.getRolUsuario());

        String sql = esAdminGlobal ? "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS"
                : "SELECT id_empleado, nombres, apellidos FROM EMPLEADOS WHERE id_tienda = ?";
        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
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

    private void inicializarPickersRangos() {
        cmbPickerDia.getItems().addAll("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");
        cmbPickerInicio.getItems().addAll("10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM");
        cmbPickerFin.getItems().addAll("11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM", "10:00 PM");
    }

    @FXML
    private void onEmpleadoSeleccionado() {
        String seleccionado = cmbEmpleado.getValue();
        if (seleccionado == null) {
            return;
        }

        int idEmpleado = Integer.parseInt(seleccionado.split(" - ")[0]);
        String sql = "SELECT tipo_empleado FROM EMPLEADOS WHERE id_empleado = ?";

        try (Connection connection = ConexionDB.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEmpleado);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    esFullTimeOPT = rs.getString("tipo_empleado");
                    if ("FT".equalsIgnoreCase(esFullTimeOPT)) {
                        horasObjetivo = 48;
                        lblJornadaInfo.setText("Jornada: Full-Time (Meta: 48 Horas)");
                    } else {
                        horasObjetivo = 24;
                        lblJornadaInfo.setText("Jornada: Part-Time (Meta: 24 Horas)");
                    }
                    actualizarProgresoHorasEfectivas();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void configurarTablaGrafica() {
        colTramo.setCellValueFactory(c -> c.getValue().tramoProperty());

        colLunes.setCellValueFactory(c -> c.getValue().lunesProperty());
        colMartes.setCellValueFactory(c -> c.getValue().martesProperty());
        colMiercoles.setCellValueFactory(c -> c.getValue().miercolesProperty());
        colJueves.setCellValueFactory(c -> c.getValue().juevesProperty());
        colViernes.setCellValueFactory(c -> c.getValue().viernesProperty());
        colSabado.setCellValueFactory(c -> c.getValue().sabadoProperty());
        colDomingo.setCellValueFactory(c -> c.getValue().domingoProperty());

        ObservableList<TableColumn<FilaHorarioGrafico, List<AsignacionItem>>> columnasDias
                = FXCollections.observableArrayList(colLunes, colMartes, colMiercoles, colJueves, colViernes, colSabado, colDomingo);

        for (TableColumn<FilaHorarioGrafico, List<AsignacionItem>> col : columnasDias) {
            col.setCellFactory(column -> new TableCell<FilaHorarioGrafico, List<AsignacionItem>>() {
                @Override
                protected void updateItem(List<AsignacionItem> items, boolean empty) {
                    super.updateItem(items, empty);
                    if (empty || items == null || items.isEmpty()) {
                        setGraphic(null);
                        setStyle("");
                    } else {
                        VBox contenedorBloques = new VBox(2);
                        contenedorBloques.setStyle("-fx-alignment: center;");

                        for (AsignacionItem item : items) {
                            Label lblBloque = new Label();
                            if (item.esBreak) {
                                lblBloque.setText(item.nombreEmpleado + " (BREAK)");
                                lblBloque.setStyle("-fx-background-color: #E2E3E5; -fx-text-fill: #383D41; -fx-padding: 3; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 10px; -fx-alignment: center; -fx-max-width: infinity;");
                            } else {
                                lblBloque.setText(item.nombreEmpleado);
                                String colorCss = paletaColores[item.idEmpleado % paletaColores.length];
                                lblBloque.setStyle(colorCss + " -fx-padding: 3; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 10px; -fx-alignment: center; -fx-max-width: infinity;");
                            }
                            contenedorBloques.getChildren().add(lblBloque);
                        }
                        setGraphic(contenedorBloques);
                    }
                }
            });
        }
        resetearEstructuraFilas();
    }

    private void resetearEstructuraFilas() {
        ObservableList<FilaHorarioGrafico> tramos = FXCollections.observableArrayList(
                new FilaHorarioGrafico("10:00 AM - 11:00 AM"), new FilaHorarioGrafico("11:00 AM - 12:00 PM"),
                new FilaHorarioGrafico("12:00 PM - 01:00 PM"), new FilaHorarioGrafico("01:00 PM - 02:00 PM"),
                new FilaHorarioGrafico("02:00 PM - 03:00 PM"), new FilaHorarioGrafico("03:00 PM - 04:00 PM"),
                new FilaHorarioGrafico("04:00 PM - 05:00 PM"), new FilaHorarioGrafico("05:00 PM - 06:00 PM"),
                new FilaHorarioGrafico("06:00 PM - 07:00 PM"), new FilaHorarioGrafico("07:00 PM - 08:00 PM"),
                new FilaHorarioGrafico("08:00 PM - 09:00 PM"), new FilaHorarioGrafico("09:00 PM - 10:00 PM")
        );
        tblMatrizHorario.setItems(tramos);
    }

    @FXML
    private void asignarRangoBloque() {
        String empleadoSeleccionado = cmbEmpleado.getValue();
        String dia = cmbPickerDia.getValue();
        String inicio = cmbPickerInicio.getValue();
        String fin = cmbPickerFin.getValue();

        if (empleadoSeleccionado == null || dia == null || inicio == null || fin == null) {
            return;
        }

        int idEmpleado = Integer.parseInt(empleadoSeleccionado.split(" - ")[0]);
        String nombreEmpleado = empleadoSeleccionado.split(" - ")[1].split(" ")[0];

        int idxInicio = cmbPickerInicio.getItems().indexOf(inicio);
        int idxFin = cmbPickerFin.getItems().indexOf(fin);

        if (idxInicio > idxFin) {
            return;
        }

        for (int i = idxInicio; i <= idxFin; i++) {
            tblMatrizHorario.getItems().get(i).agregarAsignacion(dia, new AsignacionItem(idEmpleado, nombreEmpleado, false));
        }

        if ("FT".equalsIgnoreCase(esFullTimeOPT)) {
            int totalTramos = (idxFin - idxInicio) + 1;
            if (totalTramos > 4) {
                int celdaBreak = idxInicio + (totalTramos / 2);
                tblMatrizHorario.getItems().get(celdaBreak).agregarAsignacion(dia, new AsignacionItem(idEmpleado, nombreEmpleado, true));
            }
        }

        tblMatrizHorario.refresh();
        actualizarProgresoHorasEfectivas();
    }

    private void actualizarProgresoHorasEfectivas() {
        String seleccionado = cmbEmpleado.getValue();
        if (seleccionado == null) {
            return;
        }

        int idEmpleadoActual = Integer.parseInt(seleccionado.split(" - ")[0]);
        String[] dias = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
        int contadorHoras = 0;

        for (String d : dias) {
            for (FilaHorarioGrafico fila : tblMatrizHorario.getItems()) {
                for (AsignacionItem asig : fila.getAsignacionesPorDia(d)) {
                    if (asig.idEmpleado == idEmpleadoActual && !asig.esBreak) {
                        contadorHoras++;
                    }
                }
            }
        }

        horasAsignadasActuales = contadorHoras;

        if (horasObjetivo > 0) {
            double ratio = (double) horasAsignadasActuales / horasObjetivo;
            progressHoras.setProgress(Math.min(ratio, 1.0));
            lblContadorHoras.setText(horasAsignadasActuales + " / " + horasObjetivo + " hrs de " + seleccionado.split(" - ")[1]);

            if (horasAsignadasActuales == horasObjetivo) {
                btnGuardar.setDisable(false);
                lblContadorHoras.setStyle("-fx-text-fill: #28A745; -fx-font-weight: bold;");
            } else {
                btnGuardar.setDisable(true);
                lblContadorHoras.setStyle("-fx-text-fill: #CC0000; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    private void guardarHorarioSemanas() {
        LocalDate fechaInicio = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        if (fechaInicio == null || fechaFin == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Por favor, seleccione el rango de fechas (Inicio y Fin) para la vigencia del horario.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (fechaInicio.isAfter(fechaFin)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "La fecha de inicio no puede ser posterior a la fecha de fin.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        String sqlCheck = "SELECT COUNT(*) FROM HORARIOS WHERE id_tienda = ? AND fecha_inicio = ? AND fecha_fin = ?";
        try (Connection connCheck = ConexionDB.getConnection(); PreparedStatement stmtCheck = connCheck.prepareStatement(sqlCheck)) {
            stmtCheck.setInt(1, SesionUsuario.getIdTiendaUsuarioConectado());
            stmtCheck.setString(2, fechaInicio.toString());
            stmtCheck.setString(3, fechaFin.toString());
            try (ResultSet rsCheck = stmtCheck.executeQuery()) {
                if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Ya existe un horario registrado para el periodo del " + fechaInicio + " al " + fechaFin + ".\n"
                            + "No es posible registrar un horario duplicado para el mismo periodo.", ButtonType.OK);
                    alert.setTitle("Periodo ya registrado");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String[] dias = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
        String sql = "INSERT INTO HORARIOS (id_empleado, id_tienda, dia_semana, hora_inicio, hora_fin, fecha_inicio, fecha_fin) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = ConexionDB.getConnection()) {
            connection.setAutoCommit(false);

            Map<Integer, Map<String, String[]>> mapaLote = new HashMap<>();

            for (String d : dias) {
                for (FilaHorarioGrafico fila : tblMatrizHorario.getItems()) {
                    List<AsignacionItem> listaAsignaciones = fila.getAsignacionesPorDia(d);

                    for (AsignacionItem asig : listaAsignaciones) {
                        int idEmp = asig.idEmpleado;
                        String tramoTxt = fila.getTramo();

                        mapaLote.putIfAbsent(idEmp, new HashMap<>());
                        mapaLote.get(idEmp).putIfAbsent(d, new String[]{null, null, fechaInicio.toString(), fechaFin.toString()});

                        if (mapaLote.get(idEmp).get(d)[0] == null) {
                            mapaLote.get(idEmp).get(d)[0] = traducirHora(tramoTxt.split(" - ")[0]);
                        }
                        mapaLote.get(idEmp).get(d)[1] = traducirHora(tramoTxt.split(" - ")[1]);
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Map.Entry<Integer, Map<String, String[]>> empEntry : mapaLote.entrySet()) {
                    int idEmp = empEntry.getKey();
                    for (Map.Entry<String, String[]> diaEntry : empEntry.getValue().entrySet()) {
                        statement.setInt(1, idEmp);
                        statement.setInt(2, SesionUsuario.getIdTiendaUsuarioConectado());
                        statement.setString(3, diaEntry.getKey());
                        statement.setString(4, diaEntry.getValue()[0]);
                        statement.setString(5, diaEntry.getValue()[1]);
                        statement.setString(6, diaEntry.getValue()[2]);
                        statement.setString(7, diaEntry.getValue()[3]);
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }

            connection.commit();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Se registró con éxito el horario para la tienda \"" + nombreTiendaActual + "\".", ButtonType.OK);
            alert.showAndWait();
            cerrarModal();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String traducirHora(String horaFmt) {
        horaFmt = horaFmt.trim();
        boolean esPM = horaFmt.contains("PM");
        String soloNumeros = horaFmt.replace("AM", "").replace("PM", "").trim();
        int horaInt = Integer.parseInt(soloNumeros.split(":")[0]);
        String minutos = soloNumeros.split(":")[1];
        if (esPM && horaInt != 12) {
            horaInt += 12;
        }
        if (!esPM && horaInt == 12) {
            horaInt = 0;
        }
        return String.format("%02d:%s:00", horaInt, minutos);
    }

    @FXML
    private void exportarCronogramaAExcel() {
        if (tblMatrizHorario.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No hay datos en el cronograma actual para exportar.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar Cronograma Semanal");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Hoja de cálculo de Excel (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("Cronograma_Semanal_" + nombreTiendaActual.replace(" ", "_") + ".csv");

        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return;
        }

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            writer.write('\ufeff');

            writer.write("COOLBOX - CONTROL DE PLANIFICACIÓN SEMANAL\n");
            writer.write("SUCURSAL:;" + nombreTiendaActual.toUpperCase() + "\n\n");
            writer.write("Tramo Horario;Lunes;Martes;Miercoles;Jueves;Viernes;Sabado;Domingo\n");

            String[] dias = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};

            for (FilaHorarioGrafico fila : tblMatrizHorario.getItems()) {
                StringBuilder lineaFila = new StringBuilder(fila.getTramo());

                for (String d : dias) {
                    List<AsignacionItem> asignacionesCelda = fila.getAsignacionesPorDia(d);

                    if (asignacionesCelda.isEmpty()) {
                        lineaFila.append(";[ Libre ]");
                    } else {
                        StringBuilder subTextoEmpleados = new StringBuilder(";");
                        for (int i = 0; i < asignacionesCelda.size(); i++) {
                            AsignacionItem asig = asignacionesCelda.get(i);
                            if (asig.esBreak) {
                                subTextoEmpleados.append(asig.nombreEmpleado).append(" (BREAK)");
                            } else {
                                subTextoEmpleados.append(asig.nombreEmpleado);
                            }
                            if (i < asignacionesCelda.size() - 1) {
                                subTextoEmpleados.append(" / ");
                            }
                        }
                        lineaFila.append(subTextoEmpleados);
                    }
                }
                writer.write(lineaFila.toString() + "\n");
            }

            Alert éxitoAlert = new Alert(Alert.AlertType.INFORMATION, "¡Cronograma exportado con éxito!\nEl archivo se guardó en: " + file.getAbsolutePath(), ButtonType.OK);
            éxitoAlert.setTitle("Exportación exitosa");
            éxitoAlert.setHeaderText(null);
            éxitoAlert.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Ocurrió un problema al intentar escribir el archivo: " + e.getMessage(), ButtonType.OK);
            errorAlert.showAndWait();
        }
    }

    @FXML
    private void limpiarMatriz() {
        resetearEStructureFilas();
        actualizarProgresoHorasEfectivas();
    }

    private void resetearEStructureFilas() {
        resetearEstructuraFilas();
    }

    @FXML
    private void cerrarModal() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }

    public static class AsignacionItem {

        public int idEmpleado;
        public String nombreEmpleado;
        public boolean esBreak;

        public AsignacionItem(int idEmpleado, String nombreEmpleado, boolean esBreak) {
            this.idEmpleado = idEmpleado;
            this.nombreEmpleado = nombreEmpleado;
            this.esBreak = esBreak;
        }
    }

    public static class FilaHorarioGrafico {

        private final SimpleStringProperty tramo;
        private final SimpleObjectProperty<List<AsignacionItem>> lunes, martes, miercoles, jueves, viernes, sabado, domingo;

        public FilaHorarioGrafico(String tramo) {
            this.tramo = new SimpleStringProperty(tramo);
            this.lunes = new SimpleObjectProperty<>(new ArrayList<>());
            this.martes = new SimpleObjectProperty<>(new ArrayList<>());
            this.miercoles = new SimpleObjectProperty<>(new ArrayList<>());
            this.jueves = new SimpleObjectProperty<>(new ArrayList<>());
            this.viernes = new SimpleObjectProperty<>(new ArrayList<>());
            this.sabado = new SimpleObjectProperty<>(new ArrayList<>());
            this.domingo = new SimpleObjectProperty<>(new ArrayList<>());
        }

        public String getTramo() {
            return tramo.get();
        }

        public SimpleStringProperty tramoProperty() {
            return tramo;
        }

        public SimpleObjectProperty<List<AsignacionItem>> lunesProperty() {
            return lunes;
        }

        public SimpleObjectProperty<List<AsignacionItem>> martesProperty() {
            return martes;
        }

        public SimpleObjectProperty<List<AsignacionItem>> miercolesProperty() {
            return miercoles;
        }

        public SimpleObjectProperty<List<AsignacionItem>> juevesProperty() {
            return jueves;
        }

        public SimpleObjectProperty<List<AsignacionItem>> viernesProperty() {
            return viernes;
        }

        public SimpleObjectProperty<List<AsignacionItem>> sabadoProperty() {
            return sabado;
        }

        public SimpleObjectProperty<List<AsignacionItem>> domingoProperty() {
            return domingo;
        }

        public void agregarAsignacion(String nombreDia, AsignacionItem item) {
            List<AsignacionItem> lista = getAsignacionesPorDia(nombreDia);
            lista.removeIf(asig -> asig.idEmpleado == item.idEmpleado);
            lista.add(item);
        }

        public List<AsignacionItem> getAsignacionesPorDia(String dia) {
            if ("Lunes".equalsIgnoreCase(dia)) {
                return lunes.get();
            }
            if ("Martes".equalsIgnoreCase(dia)) {
                return martes.get();
            }
            if ("Miercoles".equalsIgnoreCase(dia)) {
                return miercoles.get();
            }
            if ("Jueves".equalsIgnoreCase(dia)) {
                return jueves.get();
            }
            if ("Viernes".equalsIgnoreCase(dia)) {
                return viernes.get();
            }
            if ("Sabado".equalsIgnoreCase(dia)) {
                return sabado.get();
            }
            if ("Domingo".equalsIgnoreCase(dia)) {
                return domingo.get();
            }
            return new ArrayList<>();
        }
    }
}
