package coolbox.sistema.Modelos;

public class Usuario {
    private int idUsuario;
    private int idEmpleado;
    private String nombreUsuario;
    private String correo;
    private String estado;

    public Usuario() {
    }

    public Usuario(int idUsuario, int idEmpleado, String nombreUsuario, String correo, String estado) {
        this.idUsuario = idUsuario;
        this.idEmpleado = idEmpleado;
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.estado = estado;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return nombreUsuario;
    }
}
