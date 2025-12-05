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

@WebServlet(name = "ProductoServlet", urlPatterns = {"/api/productos"})
public class ProductoServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("usuario") != null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        String idParam = request.getParameter("id");
        String busqueda = request.getParameter("busqueda");
        
        if (idParam != null) {
            getProductoById(idParam, out, response);
        } else if (busqueda != null) {
            buscarProductos(busqueda, out, response);
        } else {
            listarProductos(out, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        crearProducto(request, out, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        actualizarProducto(request, out, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        eliminarProducto(request, out, response);
    }

    private void listarProductos(PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> productos = new ArrayList<>();
        
        try {
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idproducto, nombre, precioProducto, stock FROM producto ORDER BY idproducto";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> producto = new HashMap<>();
                producto.put("idproducto", rs.getInt("idproducto"));
                producto.put("nombre", sanitizeOutput(rs.getString("nombre")));
                producto.put("precioProducto", rs.getDouble("precioProducto"));
                producto.put("stock", rs.getInt("stock"));
                productos.add(producto);
            }
            
            resultado.put("success", true);
            resultado.put("data", productos);
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al listar productos: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void getProductoById(String id, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            int idproducto = Integer.parseInt(id);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idproducto, nombre, precioProducto, stock FROM producto WHERE idproducto = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idproducto);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> producto = new HashMap<>();
                producto.put("idproducto", rs.getInt("idproducto"));
                producto.put("nombre", sanitizeOutput(rs.getString("nombre")));
                producto.put("precioProducto", rs.getDouble("precioProducto"));
                producto.put("stock", rs.getInt("stock"));
                
                resultado.put("success", true);
                resultado.put("data", producto);
            } else {
                resultado.put("success", false);
                resultado.put("message", "Producto no encontrado");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (NumberFormatException e) {
            resultado.put("success", false);
            resultado.put("message", "ID de producto inválido");
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al obtener producto: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void buscarProductos(String busqueda, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> productos = new ArrayList<>();
        
        try {
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT idproducto, nombre, precioProducto, stock FROM producto " +
                        "WHERE nombre LIKE ? ORDER BY nombre";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + busqueda + "%");
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> producto = new HashMap<>();
                producto.put("idproducto", rs.getInt("idproducto"));
                producto.put("nombre", sanitizeOutput(rs.getString("nombre")));
                producto.put("precioProducto", rs.getDouble("precioProducto"));
                producto.put("stock", rs.getInt("stock"));
                productos.add(producto);
            }
            
            resultado.put("success", true);
            resultado.put("data", productos);
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al buscar productos: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void crearProducto(HttpServletRequest request, PrintWriter out, HttpServletResponse response) throws IOException {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            String nombre = jsonObject.get("nombre").getAsString();
            double precio = jsonObject.get("precioProducto").getAsDouble();
            int stock = jsonObject.get("stock").getAsInt();
            
            if (nombre == null || nombre.trim().isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "El nombre es requerido");
                out.print(gson.toJson(resultado));
                return;
            }
            
            if (precio <= 0) {
                resultado.put("success", false);
                resultado.put("message", "El precio debe ser mayor a 0");
                out.print(gson.toJson(resultado));
                return;
            }
            
            if (stock < 0) {
                resultado.put("success", false);
                resultado.put("message", "El stock no puede ser negativo");
                out.print(gson.toJson(resultado));
                return;
            }
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "INSERT INTO producto (nombre, precioProducto, stock) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, sanitizeInput(nombre));
            ps.setDouble(2, precio);
            ps.setInt(3, stock);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    resultado.put("success", true);
                    resultado.put("message", "Producto creado exitosamente");
                    resultado.put("idproducto", newId);
                }
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al crear producto: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void actualizarProducto(HttpServletRequest request, PrintWriter out, HttpServletResponse response) throws IOException {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            int id = jsonObject.get("idproducto").getAsInt();
            String nombre = jsonObject.get("nombre").getAsString();
            double precio = jsonObject.get("precioProducto").getAsDouble();
            int stock = jsonObject.get("stock").getAsInt();
            
            if (nombre == null || nombre.trim().isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "El nombre es requerido");
                out.print(gson.toJson(resultado));
                return;
            }
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "UPDATE producto SET nombre = ?, precioProducto = ?, stock = ? WHERE idproducto = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, sanitizeInput(nombre));
            ps.setDouble(2, precio);
            ps.setInt(3, stock);
            ps.setInt(4, id);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                resultado.put("success", true);
                resultado.put("message", "Producto actualizado exitosamente");
            } else {
                resultado.put("success", false);
                resultado.put("message", "Producto no encontrado");
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al actualizar producto: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void eliminarProducto(HttpServletRequest request, PrintWriter out, HttpServletResponse response) throws IOException {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String id = request.getParameter("id");
            int idproducto = Integer.parseInt(id);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "DELETE FROM producto WHERE idproducto = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idproducto);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                resultado.put("success", true);
                resultado.put("message", "Producto eliminado exitosamente");
            } else {
                resultado.put("success", false);
                resultado.put("message", "Producto no encontrado");
            }
            
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al eliminar producto: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
    }

    private String sanitizeOutput(String output) {
        if (output == null) return null;
        return output.replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;");
    }
}
