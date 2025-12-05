package Servlet;

import Configuracion.CConexion;
import Modelo.ModeloUsuario;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/api/login", "/api/logout", "/api/session"})
public class LoginServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        String pathInfo = request.getServletPath();
        
        if (pathInfo.equals("/api/login")) {
            handleLogin(request, response);
        } else if (pathInfo.equals("/api/logout")) {
            handleLogout(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        if (request.getServletPath().equals("/api/session")) {
            checkSession(request, response);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        PrintWriter out = response.getWriter();
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            
            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "Usuario y contraseña son requeridos");
                out.print(gson.toJson(resultado));
                return;
            }
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            String sql = "SELECT idusuario, username, nombre, rol, activo FROM usuario WHERE username = ? AND password = ? AND activo = true";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                ModeloUsuario usuario = new ModeloUsuario();
                usuario.setIdusuario(rs.getInt("idusuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));
                
                HttpSession session = request.getSession(true);
                session.setAttribute("usuario", usuario);
                session.setAttribute("idusuario", usuario.getIdusuario());
                session.setAttribute("username", usuario.getUsername());
                session.setAttribute("nombre", usuario.getNombre());
                session.setAttribute("rol", usuario.getRol());
                session.setMaxInactiveInterval(1800);
                
                resultado.put("success", true);
                resultado.put("message", "Login exitoso");
                resultado.put("usuario", Map.of(
                    "idusuario", usuario.getIdusuario(),
                    "username", usuario.getUsername(),
                    "nombre", usuario.getNombre(),
                    "rol", usuario.getRol()
                ));
                
            } else {
                resultado.put("success", false);
                resultado.put("message", "Usuario o contraseña incorrectos");
            }
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        PrintWriter out = response.getWriter();
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            
            resultado.put("success", true);
            resultado.put("message", "Sesión cerrada exitosamente");
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al cerrar sesión");
        }
        
        out.print(gson.toJson(resultado));
    }

    private void checkSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        PrintWriter out = response.getWriter();
        Map<String, Object> resultado = new HashMap<>();
        
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("usuario") != null) {
            resultado.put("authenticated", true);
            resultado.put("usuario", Map.of(
                "idusuario", session.getAttribute("idusuario"),
                "username", session.getAttribute("username"),
                "nombre", session.getAttribute("nombre"),
                "rol", session.getAttribute("rol")
            ));
        } else {
            resultado.put("authenticated", false);
        }
        
        out.print(gson.toJson(resultado));
    }
}

