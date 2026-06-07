package coolbox.sistema.Modelos;

public class Cuadre {
    private int idCuadre;
    private int idTienda;
    private int idEmpleado;
    private String fecha;
    private double montoTotal;
    private String observaciones;

    public Cuadre() {
    }

    public int getIdCuadre() {
        return idCuadre;
    }

    public void setIdCuadre(int idCuadre) {
        this.idCuadre = idCuadre;
    }

    public int getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(int idTienda) {
        this.idTienda = idTienda;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}