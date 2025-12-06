package Servlet;

import Configuracion.CConexion;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
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

@WebServlet(name = "VentaServlet", urlPatterns = { "/api/ventas" })
public class VentaServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("usuario") != null;
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

        procesarVenta(request, out, response);
    }

    private void procesarVenta(HttpServletRequest request, PrintWriter out, HttpServletResponse response)
            throws IOException {

        Map<String, Object> resultado = new HashMap<>();
        Connection conn = null;

        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            int idcliente = jsonObject.get("idcliente").getAsInt();
            JsonArray detallesArray = jsonObject.getAsJsonArray("detalles");

            if (detallesArray == null || detallesArray.size() == 0) {
                resultado.put("success", false);
                resultado.put("message", "Debe agregar al menos un producto");
                out.print(gson.toJson(resultado));
                return;
            }

            CConexion conexion = new CConexion();
            conn = conexion.estableceConexion();

            // Iniciar transacción
            conn.setAutoCommit(false);

            try {
                // 1. Insertar factura
                String sqlFactura = "INSERT INTO factura (fechaFactura, fkcliente) VALUES (CURDATE(), ?)";
                PreparedStatement psFactura = conn.prepareStatement(sqlFactura,
                        PreparedStatement.RETURN_GENERATED_KEYS);
                psFactura.setInt(1, idcliente);
                psFactura.executeUpdate();

                ResultSet generatedKeys = psFactura.getGeneratedKeys();
                int idfactura = 0;
                if (generatedKeys.next()) {
                    idfactura = generatedKeys.getInt(1);
                }
                psFactura.close();

                // 2. Insertar detalles y actualizar stock
                String sqlDetalle = "INSERT INTO detalle (fkfactura, fkproducto, cantidad, precioVenta) VALUES (?, ?, ?, ?)";
                // String sqlUpdateStock = "UPDATE producto SET stock = stock - ? WHERE
                // idproducto = ?";
                String sqlCheckStock = "SELECT stock FROM producto WHERE idproducto = ?";

                PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                // PreparedStatement psUpdateStock = conn.prepareStatement(sqlUpdateStock);
                PreparedStatement psCheckStock = conn.prepareStatement(sqlCheckStock);

                double totalVenta = 0;

                for (int i = 0; i < detallesArray.size(); i++) {
                    JsonObject detalle = detallesArray.get(i).getAsJsonObject();
                    int idproducto = detalle.get("idproducto").getAsInt();
                    int cantidad = detalle.get("cantidad").getAsInt();
                    double precioVenta = detalle.get("precioVenta").getAsDouble();

                    // Verificar stock disponible
                    psCheckStock.setInt(1, idproducto);
                    ResultSet rsStock = psCheckStock.executeQuery();
                    if (rsStock.next()) {
                        int stockDisponible = rsStock.getInt("stock");
                        if (stockDisponible < cantidad) {
                            throw new Exception("Stock insuficiente para producto ID: " + idproducto);
                        }
                    }
                    rsStock.close();

                    // Insertar detalle
                    psDetalle.setInt(1, idfactura);
                    psDetalle.setInt(2, idproducto);
                    psDetalle.setInt(3, cantidad);
                    psDetalle.setDouble(4, precioVenta);
                    psDetalle.executeUpdate();

                    // Actualizar stock
                    // psUpdateStock.setInt(1, cantidad);
                    // psUpdateStock.setInt(2, idproducto);
                    // psUpdateStock.executeUpdate();

                    totalVenta += cantidad * precioVenta;
                }

                psDetalle.close();
                // psUpdateStock.close();
                psCheckStock.close();

                // Confirmar transacción
                conn.commit();

                resultado.put("success", true);
                resultado.put("message", "Venta procesada exitosamente");
                resultado.put("idfactura", idfactura);
                resultado.put("total", totalVenta);

            } catch (Exception e) {
                // Revertir transacción en caso de error
                if (conn != null) {
                    conn.rollback();
                }
                throw e;
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            }

            conexion.cerrarConexion();

        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al procesar venta: " + e.getMessage());
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
        return "Servlet REST para gestión de ventas con transacciones";
    }
}
