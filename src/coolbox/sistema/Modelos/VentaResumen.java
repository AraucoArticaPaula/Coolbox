package coolbox.sistema.Modelos;

import java.util.List;

public class VentaResumen {
    private int idVenta;
    private double totalComisiones;
    private String fechaVenta;
    private String numeroBoleta;
    private double montoVenta;
    private int cantidadItems;
    private String nombreVendedor;
    private List<ComisionResumen> desglose;

    public VentaResumen(int idVenta, double totalComisiones, String fechaVenta,
                        String numeroBoleta, double montoVenta, int cantidadItems,
                        String nombreVendedor, List<ComisionResumen> desglose) {
        this.idVenta = idVenta;
        this.totalComisiones = totalComisiones;
        this.fechaVenta = fechaVenta;
        this.numeroBoleta = numeroBoleta;
        this.montoVenta = montoVenta;
        this.cantidadItems = cantidadItems;
        this.nombreVendedor = nombreVendedor;
        this.desglose = desglose;
    }

    public int getIdVenta() { return idVenta; }
    public double getTotalComisiones() { return totalComisiones; }
    public String getFechaVenta() { return fechaVenta; }
    public String getNumeroBoleta() { return numeroBoleta; }
    public double getMontoVenta() { return montoVenta; }
    public int getCantidadItems() { return cantidadItems; }
    public String getNombreVendedor() { return nombreVendedor; }
    public List<ComisionResumen> getDesglose() { return desglose; }
}