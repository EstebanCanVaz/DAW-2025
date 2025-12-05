package Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;

public class CConexion {
    private Connection conectar = null;
    
    private String usuario = "root";
    private String contrasena = "";
    private String bd = "dbpos1";
    private String ip = "localhost";
    private String puerto = "3306";
    private String cadena = "jdbc:mysql://" + ip + ":" + puerto + "/" + bd;
    
    public Connection estableceConexion() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conectar = DriverManager.getConnection(cadena, usuario, contrasena);
        } catch(Exception e) {
            System.err.println("Error de conexión a BD: " + e.getMessage());
        }
        return conectar;
    }
    
    public void cerrarConexion() {
        try {
            if(conectar != null && !conectar.isClosed()) {
                conectar.close();
            }
        } catch(Exception e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}
