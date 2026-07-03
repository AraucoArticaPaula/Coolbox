package coolbox.sistema.Modelos;

public class Producto {
    private int idProducto;
    private String descripcion;
    private double precio;

    public Producto() {
    }

    public Producto(int idProducto, String descripcion, double precio) {
        this.idProducto = idProducto;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    @Override
    public String toString() {
        return (descripcion != null) ? descripcion : "Sin descripción";
    }
}