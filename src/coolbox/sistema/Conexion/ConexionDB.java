package coolbox.sistema.Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String URL =
        "jdbc:sqlserver://localhost;databaseName=CoolboxDB;encrypt=false;trustServerCertificate=true;";
    private static final String USER = "sa";        // tu usuario SQL Server
    private static final String PASSWORD = "12345"; // tu contraseña SQL Server

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}