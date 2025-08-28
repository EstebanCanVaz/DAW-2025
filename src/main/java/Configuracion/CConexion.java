/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

/**
 *
 * @author Esteban Canto Vázquez
 */
public class CConexion {
    Connection conectar = null;
    
    String usuario="root";
    String contrasena="Computadora12";
    String bd="dbpos";
    String ip="localhost";
    String puerto="3306";
    
    String cadena="jdbc:mysql://"+ip+":"+puerto+"/"+bd;
    
    public Connection estableceConexion(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conectar = DriverManager.getConnection(cadena,usuario,contrasena);
            //JOptionPane.showMessageDialog(null, "Conexión correcta a BD");
        }
        catch(Exception e1){
            JOptionPane.showMessageDialog(null, "No se conectó a la BD: "+e1.toString());
        }
        return conectar;
    }
    
    public void cerrarConexion(){
        try{
            if(conectar!=null && !conectar.isClosed()){
                conectar.close();
                //JOptionPane.showMessageDialog(null, "La conexión fue cerrada");
            }
        }
        catch(Exception e1){
            JOptionPane.showMessageDialog(null, "No se logró cerrar la conexión: "+e1.toString());
        }
    }
}
