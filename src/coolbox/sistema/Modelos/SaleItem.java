package coolbox.sistema.Modelos;

public class SaleItem {
    private int idProducto;
    private String descripcion;
    private double precio;

    public SaleItem() {
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
        return (this.descripcion != null && !this.descripcion.isBlank()) 
                ? this.descripcion 
                : "Producto #" + this.idProducto;
    }
}