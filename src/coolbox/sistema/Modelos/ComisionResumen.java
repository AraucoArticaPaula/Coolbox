package coolbox.sistema.Modelos;

public class ComisionResumen {
    private String tipo;
    private double total;

    public ComisionResumen() {
    }

    public ComisionResumen(String tipo, double total) {
        this.tipo = tipo;
        this.total = total;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
