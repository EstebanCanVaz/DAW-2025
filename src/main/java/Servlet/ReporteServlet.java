package Servlet;

import Configuracion.CConexion;
import com.google.gson.Gson;
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

@WebServlet(name = "ReporteServlet", urlPatterns = {"/api/reportes"})
public class ReporteServlet extends HttpServlet {

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
        
        String action = request.getParameter("action");
        
        if ("factura".equals(action)) {
            buscarFactura(request, out, response);
        } else if ("fechas".equals(action)) {
            reportePorFechas(request, out, response);
        } else if ("dashboard".equals(action)) {
            getDashboardStats(request, out, response);
        } else {
            sendError(out, response, "Acción no válida", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void buscarFactura(HttpServletRequest request, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String idParam = request.getParameter("id");
            int idfactura = Integer.parseInt(idParam);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            // Obtener información de la factura
            String sqlFactura = "SELECT f.idfactura, f.fechaFactura, " +
                               "c.nombre, c.appaterno, c.apmaterno " +
                               "FROM factura f " +
                               "INNER JOIN cliente c ON c.idcliente = f.fkcliente " +
                               "WHERE f.idfactura = ?";
            
            PreparedStatement psFactura = conn.prepareStatement(sqlFactura);
            psFactura.setInt(1, idfactura);
            ResultSet rsFactura = psFactura.executeQuery();
            
            if (rsFactura.next()) {
                Map<String, Object> factura = new HashMap<>();
                factura.put("idfactura", rsFactura.getInt("idfactura"));
                factura.put("fechaFactura", rsFactura.getString("fechaFactura"));
                factura.put("cliente", rsFactura.getString("nombre") + " " + 
                           rsFactura.getString("appaterno") + " " + 
                           rsFactura.getString("apmaterno"));
                
                // Obtener detalles de la factura
                String sqlDetalles = "SELECT p.nombre, d.cantidad, d.precioVenta " +
                                    "FROM detalle d " +
                                    "INNER JOIN producto p ON p.idproducto = d.fkproducto " +
                                    "WHERE d.fkfactura = ?";
                
                PreparedStatement psDetalles = conn.prepareStatement(sqlDetalles);
                psDetalles.setInt(1, idfactura);
                ResultSet rsDetalles = psDetalles.executeQuery();
                
                List<Map<String, Object>> detalles = new ArrayList<>();
                double total = 0;
                
                while (rsDetalles.next()) {
                    Map<String, Object> detalle = new HashMap<>();
                    detalle.put("producto", rsDetalles.getString("nombre"));
                    detalle.put("cantidad", rsDetalles.getInt("cantidad"));
                    detalle.put("precioVenta", rsDetalles.getDouble("precioVenta"));
                    double subtotal = rsDetalles.getInt("cantidad") * rsDetalles.getDouble("precioVenta");
                    detalle.put("subtotal", subtotal);
                    total += subtotal;
                    detalles.add(detalle);
                }
                
                factura.put("detalles", detalles);
                factura.put("total", total);
                
                resultado.put("success", true);
                resultado.put("data", factura);
                
                rsDetalles.close();
                psDetalles.close();
            } else {
                resultado.put("success", false);
                resultado.put("message", "Factura no encontrada");
            }
            
            rsFactura.close();
            psFactura.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al buscar factura: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void reportePorFechas(HttpServletRequest request, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String fechaInicio = request.getParameter("fechaInicio");
            String fechaFin = request.getParameter("fechaFin");
            
            if (fechaInicio == null || fechaFin == null) {
                resultado.put("success", false);
                resultado.put("message", "Fechas de inicio y fin son requeridas");
                out.print(gson.toJson(resultado));
                return;
            }
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT f.idfactura, f.fechaFactura, " +
                        "CONCAT(c.nombre, ' ', c.appaterno, ' ', c.apmaterno) as cliente, " +
                        "p.nombre as producto, d.cantidad, d.precioVenta, " +
                        "(d.cantidad * d.precioVenta) as total " +
                        "FROM detalle d " +
                        "INNER JOIN factura f ON f.idfactura = d.fkfactura " +
                        "INNER JOIN cliente c ON c.idcliente = f.fkcliente " +
                        "INNER JOIN producto p ON p.idproducto = d.fkproducto " +
                        "WHERE f.fechaFactura BETWEEN ? AND ? " +
                        "ORDER BY f.fechaFactura DESC, f.idfactura";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);
            ResultSet rs = ps.executeQuery();
            
            List<Map<String, Object>> ventas = new ArrayList<>();
            double totalGeneral = 0;
            
            while (rs.next()) {
                Map<String, Object> venta = new HashMap<>();
                venta.put("idfactura", rs.getInt("idfactura"));
                venta.put("fechaFactura", rs.getString("fechaFactura"));
                venta.put("cliente", rs.getString("cliente"));
                venta.put("producto", rs.getString("producto"));
                venta.put("cantidad", rs.getInt("cantidad"));
                venta.put("precioVenta", rs.getDouble("precioVenta"));
                venta.put("total", rs.getDouble("total"));
                totalGeneral += rs.getDouble("total");
                ventas.add(venta);
            }
            
            resultado.put("success", true);
            resultado.put("data", ventas);
            resultado.put("totalGeneral", totalGeneral);
            resultado.put("totalVentas", ventas.size());
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al generar reporte: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void getDashboardStats(HttpServletRequest request, PrintWriter out, HttpServletResponse response) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            // Total de productos
            String sqlProductos = "SELECT COUNT(*) as total FROM producto";
            PreparedStatement psProductos = conn.prepareStatement(sqlProductos);
            ResultSet rsProductos = psProductos.executeQuery();
            int totalProductos = 0;
            if (rsProductos.next()) {
                totalProductos = rsProductos.getInt("total");
            }
            rsProductos.close();
            psProductos.close();
            
            // Total de clientes
            String sqlClientes = "SELECT COUNT(*) as total FROM cliente";
            PreparedStatement psClientes = conn.prepareStatement(sqlClientes);
            ResultSet rsClientes = psClientes.executeQuery();
            int totalClientes = 0;
            if (rsClientes.next()) {
                totalClientes = rsClientes.getInt("total");
            }
            rsClientes.close();
            psClientes.close();
            
            // Ventas de hoy
            String sqlVentasHoy = "SELECT COUNT(*) as total, " +
                                 "COALESCE(SUM(d.cantidad * d.precioVenta), 0) as totalMonto " +
                                 "FROM factura f " +
                                 "LEFT JOIN detalle d ON d.fkfactura = f.idfactura " +
                                 "WHERE f.fechaFactura = CURDATE()";
            PreparedStatement psVentasHoy = conn.prepareStatement(sqlVentasHoy);
            ResultSet rsVentasHoy = psVentasHoy.executeQuery();
            int ventasHoy = 0;
            double montoHoy = 0;
            if (rsVentasHoy.next()) {
                ventasHoy = rsVentasHoy.getInt("total");
                montoHoy = rsVentasHoy.getDouble("totalMonto");
            }
            rsVentasHoy.close();
            psVentasHoy.close();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProductos", totalProductos);
            stats.put("totalClientes", totalClientes);
            stats.put("ventasHoy", ventasHoy);
            stats.put("montoHoy", montoHoy);
            
            resultado.put("success", true);
            resultado.put("data", stats);
            
            conexion.cerrarConexion();
            
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al obtener estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
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
        return "Servlet REST para reportes y estadísticas";
    }
}



