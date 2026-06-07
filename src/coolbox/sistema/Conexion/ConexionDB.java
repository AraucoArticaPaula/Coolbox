package coolbox.sistema.Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String URL = 
        "jdbc:sqlserver://localhost;databaseName=CoolboxDB;integratedSecurity=true;encrypt=false;trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}