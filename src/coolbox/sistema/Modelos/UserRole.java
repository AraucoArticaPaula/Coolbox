package coolbox.sistema.Modelos;

public class UserRole {
    private String usuario;
    private String rol;

    public UserRole() {
    }

    public UserRole(String usuario, String rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
