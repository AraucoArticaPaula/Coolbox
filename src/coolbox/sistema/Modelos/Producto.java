package coolbox.sistema.Modelos;

public class Producto {
    private int idProducto;
    private String descripcion;
    private double precio;

    // Constructor vacío (necesario para frameworks de UI o instanciación simple)
    public Producto() {
    }

    // Constructor con parámetros (útil para crear objetos rápidamente)
    public Producto(int idProducto, String descripcion, double precio) {
        this.idProducto = idProducto;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    // Getters y Setters
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

    // Este es el método clave para que tu ComboBox muestre el nombre correctamente
    @Override
    public String toString() {
        return (descripcion != null) ? descripcion : "Sin descripción";
    }
}