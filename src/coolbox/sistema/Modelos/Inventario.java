package coolbox.sistema.Modelos;

import javafx.beans.property.*;

public class Inventario {
    private final IntegerProperty idInventario = new SimpleIntegerProperty();
    private final IntegerProperty idTienda = new SimpleIntegerProperty();
    private final IntegerProperty idProducto = new SimpleIntegerProperty();
    private final StringProperty descripcionProducto = new SimpleStringProperty();
    private final IntegerProperty stockActual = new SimpleIntegerProperty();
    private final StringProperty ultimaFecha = new SimpleStringProperty();

    public Inventario() {
    }

    // --- Getters para las propiedades (Requeridos por PropertyValueFactory) ---

    public IntegerProperty idInventarioProperty() { return idInventario; }
    public int getIdInventario() { return idInventario.get(); }
    public void setIdInventario(int id) { this.idInventario.set(id); }

    public IntegerProperty idTiendaProperty() { return idTienda; }
    public int getIdTienda() { return idTienda.get(); }
    public void setIdTienda(int id) { this.idTienda.set(id); }

    public IntegerProperty idProductoProperty() { return idProducto; }
    public int getIdProducto() { return idProducto.get(); }
    public void setIdProducto(int id) { this.idProducto.set(id); }

    public StringProperty descripcionProductoProperty() { return descripcionProducto; }
    public String getDescripcionProducto() { return descripcionProducto.get(); }
    public void setDescripcionProducto(String desc) { this.descripcionProducto.set(desc); }

    public IntegerProperty stockActualProperty() { return stockActual; }
    public int getStockActual() { return stockActual.get(); }
    public void setStockActual(int stock) { this.stockActual.set(stock); }

    public StringProperty ultimaFechaProperty() { return ultimaFecha; }
    public String getUltimaFecha() { return ultimaFecha.get(); }
    public void setUltimaFecha(String fecha) { this.ultimaFecha.set(fecha); }
}