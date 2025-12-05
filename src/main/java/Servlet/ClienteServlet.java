package Servlet;

import Configuracion.CConexion;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "ClienteServlet", urlPatterns = {"/api/clientes"})
public class ClienteServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("usuario") != null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            sendError(out, response, "No autenticado", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String idParam = request.getParameter("id");
        String busqueda = request.getParameter("busqueda");
        
        if (idParam != null) {
            getClienteById(idParam, out, response);
        } else if (busqueda != null) {
            buscarClientes(busqueda, out, response);
        } else {
            listarClientes(out, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            sendError(out, response, "No autenticado", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        crearCliente(request, out, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            sendError(out, response, "No autenticado", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        actualizarCliente(request, out, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            sendError(out, response, "No autenticado", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        eliminarCliente(request, out, response);
    }

    private void listarClientes(PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> clientes = new ArrayList<>();
        
        try {
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idcliente, nombre, appaterno, apmaterno FROM cliente ORDER BY idcliente";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> cliente = new HashMap<>();
                cliente.put("idcliente", rs.getInt("idcliente"));
                cliente.put("nombre", sanitize(rs.getString("nombre")));
                cliente.put("appaterno", sanitize(rs.getString("appaterno")));
                cliente.put("apmaterno", sanitize(rs.getString("apmaterno")));
                clientes.add(cliente);
            }
            
            resultado.put("success", true);
            resultado.put("data", clientes);
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al listar clientes: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private void getClienteById(String id, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            int idcliente = Integer.parseInt(id);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idcliente, nombre, appaterno, apmaterno FROM cliente WHERE idcliente = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idcliente);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> cliente = new HashMap<>();
                cliente.put("idcliente", rs.getInt("idcliente"));
                cliente.put("nombre", sanitize(rs.getString("nombre")));
                cliente.put("appaterno", sanitize(rs.getString("appaterno")));
                cliente.put("apmaterno", sanitize(rs.getString("apmaterno")));
                
                resultado.put("success", true);
                resultado.put("data", cliente);
            } else {
                resultado.put("success", false);
                resultado.put("message", "Cliente no encontrado");
            }
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al obtener cliente: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private void buscarClientes(String busqueda, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> clientes = new ArrayList<>();
        
        try {
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idcliente, nombre, appaterno, apmaterno FROM cliente " +
                        "WHERE nombre LIKE ? OR appaterno LIKE ? OR apmaterno LIKE ? ORDER BY nombre";
            PreparedStatement ps = conn.prepareStatement(sql);
            String searchTerm = "%" + busqueda + "%";
            ps.setString(1, searchTerm);
            ps.setString(2, searchTerm);
            ps.setString(3, searchTerm);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> cliente = new HashMap<>();
                cliente.put("idcliente", rs.getInt("idcliente"));
                cliente.put("nombre", sanitize(rs.getString("nombre")));
                cliente.put("appaterno", sanitize(rs.getString("appaterno")));
                cliente.put("apmaterno", sanitize(rs.getString("apmaterno")));
                clientes.add(cliente);
            }
            
            resultado.put("success", true);
            resultado.put("data", clientes);
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al buscar clientes: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private void crearCliente(HttpServletRequest request, PrintWriter out, HttpServletResponse response)
            throws IOException {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            String nombre = jsonObject.get("nombre").getAsString();
            String appaterno = jsonObject.get("appaterno").getAsString();
            String apmaterno = jsonObject.get("apmaterno").getAsString();
            
            if (nombre == null || nombre.trim().isEmpty() ||
                appaterno == null || appaterno.trim().isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "Nombre y apellido paterno son requeridos");
                out.print(gson.toJson(resultado));
                return;
            }
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "INSERT INTO cliente (nombre, appaterno, apmaterno) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, sanitize(nombre));
            ps.setString(2, sanitize(appaterno));
            ps.setString(3, sanitize(apmaterno));
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    resultado.put("success", true);
                    resultado.put("message", "Cliente creado exitosamente");
                    resultado.put("idcliente", generatedKeys.getInt(1));
                }
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al crear cliente: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private void actualizarCliente(HttpServletRequest request, PrintWriter out, HttpServletResponse response)
            throws IOException {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            int id = jsonObject.get("idcliente").getAsInt();
            String nombre = jsonObject.get("nombre").getAsString();
            String appaterno = jsonObject.get("appaterno").getAsString();
            String apmaterno = jsonObject.get("apmaterno").getAsString();
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "UPDATE cliente SET nombre = ?, appaterno = ?, apmaterno = ? WHERE idcliente = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, sanitize(nombre));
            ps.setString(2, sanitize(appaterno));
            ps.setString(3, sanitize(apmaterno));
            ps.setInt(4, id);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                resultado.put("success", true);
                resultado.put("message", "Cliente actualizado exitosamente");
            } else {
                resultado.put("success", false);
                resultado.put("message", "Cliente no encontrado");
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al actualizar cliente: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private void eliminarCliente(HttpServletRequest request, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String id = request.getParameter("id");
            int idcliente = Integer.parseInt(id);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "DELETE FROM cliente WHERE idcliente = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idcliente);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                resultado.put("success", true);
                resultado.put("message", "Cliente eliminado exitosamente");
            } else {
                resultado.put("success", false);
                resultado.put("message", "Cliente no encontrado");
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al eliminar cliente: " + e.getMessage());
        }
        
        out.print(gson.toJson(resultado));
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;");
    }

    private void sendError(PrintWriter out, HttpServletResponse response, String message, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        out.print(gson.toJson(error));
        response.setStatus(statusCode);
    }

    @Override
    public String getServletInfo() {
        return "Servlet REST para gestión de clientes";
    }
}
