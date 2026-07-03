package coolbox.sistema.Modelos;

public class Tienda {
    private int idTienda;
    private String nombreTienda;
    private String centroComercial;

    public Tienda() {
    }

    public Tienda(int idTienda, String nombreTienda, String centroComercial) {
        this.idTienda = idTienda;
        this.nombreTienda = nombreTienda;
        this.centroComercial = centroComercial;
    }

    public int getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(int idTienda) {
        this.idTienda = idTienda;
    }

    public String getNombreTienda() {
        return nombreTienda;
    }

    public void setNombreTienda(String nombreTienda) {
        this.nombreTienda = nombreTienda;
    }

    public String getCentroComercial() {
        return centroComercial;
    }

    public void setCentroComercial(String centroComercial) {
        this.centroComercial = centroComercial;
    }

    @Override
    public String toString() {
        return nombreTienda;
    }
}