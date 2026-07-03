package coolbox.sistema.Modelos;

public class RolePermission {
    private String rol;
    private String permiso;

    public RolePermission() {
    }

    public RolePermission(String rol, String permiso) {
        this.rol = rol;
        this.permiso = permiso;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getPermiso() {
        return permiso;
    }

    public void setPermiso(String permiso) {
        this.permiso = permiso;
    }
}
