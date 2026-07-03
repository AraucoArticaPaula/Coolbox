package coolbox.sistema.Controladores;

public class SesionUsuario {

    private static int idEmpleado;
    private static String nombreUsuario;
    private static String nombresCompletos;
    private static int idTiendaUsuarioConectado;
    private static String rolUsuario;

    public static int getIdEmpleado() {
        return idEmpleado;
    }

    public static void setIdEmpleado(int idEmpleado) {
        SesionUsuario.idEmpleado = idEmpleado;
    }

    public static String getNombreUsuario() {
        return nombreUsuario;
    }

    public static void setNombreUsuario(String nombreUsuario) {
        SesionUsuario.nombreUsuario = nombreUsuario;
    }

    public static String getNombresCompletos() {
        return nombresCompletos;
    }

    public static void setNombresCompletos(String nombresCompletos) {
        SesionUsuario.nombresCompletos = nombresCompletos;
    }

    public static int getIdTiendaUsuarioConectado() {
        return idTiendaUsuarioConectado;
    }

    public static void setIdTiendaUsuarioConectado(int idTienda) {
        SesionUsuario.idTiendaUsuarioConectado = idTienda;
    }

    public static String getRolUsuario() {
        return rolUsuario;
    }

    public static void setRolUsuario(String rol) {
        SesionUsuario.rolUsuario = rol;
    }
}
