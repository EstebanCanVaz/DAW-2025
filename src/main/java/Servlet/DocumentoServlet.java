package Servlet;

import Configuracion.CConexion;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet(name = "DocumentoServlet", urlPatterns = {"/api/documentos", "/api/documentos/download"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class DocumentoServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private static final String UPLOAD_DIRECTORY = "uploads/documentos";

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("usuario") != null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (request.getServletPath().contains("download")) {
            descargarDocumento(request, response);
        } else {
            listarDocumentos(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        subirDocumento(request, response, out);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (!isAuthenticated(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "No autenticado");
            out.print(gson.toJson(error));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        eliminarDocumento(request, out);
    }

    private void listarDocumentos(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> documentos = new ArrayList<>();
        
        try {
            String idClienteParam = request.getParameter("idcliente");
            if (idClienteParam == null || idClienteParam.isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "ID de cliente es requerido");
                out.print(gson.toJson(resultado));
                return;
            }
            
            int idcliente = Integer.parseInt(idClienteParam);
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT d.iddocumento, d.nombre_archivo, d.tipo_archivo, " +
                        "d.fecha_subida, c.nombre, c.appaterno, c.apmaterno " +
                        "FROM documento_cliente d " +
                        "INNER JOIN cliente c ON d.fkcliente = c.idcliente " +
                        "WHERE d.fkcliente = ? ORDER BY d.fecha_subida DESC";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idcliente);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("iddocumento", rs.getInt("iddocumento"));
                doc.put("nombre_archivo", rs.getString("nombre_archivo"));
                doc.put("tipo_archivo", rs.getString("tipo_archivo"));
                doc.put("fecha_subida", rs.getString("fecha_subida"));
                doc.put("cliente", rs.getString("nombre") + " " + rs.getString("appaterno") + " " + rs.getString("apmaterno"));
                documentos.add(doc);
            }
            
            resultado.put("success", true);
            resultado.put("data", documentos);
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al listar documentos: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void subirDocumento(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws IOException, ServletException {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String idClienteParam = request.getParameter("idcliente");
            if (idClienteParam == null || idClienteParam.isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "ID de cliente es requerido");
                out.print(gson.toJson(resultado));
                return;
            }
            
            int idcliente = Integer.parseInt(idClienteParam);
            Part filePart = request.getPart("archivo");
            
            if (filePart == null) {
                resultado.put("success", false);
                resultado.put("message", "No se ha seleccionado ningún archivo");
                out.print(gson.toJson(resultado));
                return;
            }
            
            String fileName = getSubmittedFileName(filePart);
            String fileType = filePart.getContentType();
            
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
            String filePath = uploadPath + File.separator + uniqueFileName;
            filePart.write(filePath);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "INSERT INTO documento_cliente (fkcliente, nombre_archivo, tipo_archivo, ruta_archivo) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idcliente);
            ps.setString(2, fileName);
            ps.setString(3, fileType);
            ps.setString(4, uniqueFileName);
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    resultado.put("success", true);
                    resultado.put("message", "Documento subido exitosamente");
                    resultado.put("iddocumento", generatedKeys.getInt(1));
                    resultado.put("nombre_archivo", fileName);
                }
            }
            
            ps.close();
            conexion.cerrarConexion();
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al subir documento: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private void descargarDocumento(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String idParam = request.getParameter("id");
            if (idParam == null || idParam.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID de documento requerido");
                return;
            }
            
            int iddocumento = Integer.parseInt(idParam);
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sql = "SELECT nombre_archivo, tipo_archivo, ruta_archivo FROM documento_cliente WHERE iddocumento = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, iddocumento);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String nombreArchivo = rs.getString("nombre_archivo");
                String tipoArchivo = rs.getString("tipo_archivo");
                String rutaArchivo = rs.getString("ruta_archivo");
                
                String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
                File file = new File(uploadPath + File.separator + rutaArchivo);
                
                if (!file.exists()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Archivo no encontrado");
                    return;
                }
                
                response.setContentType(tipoArchivo != null ? tipoArchivo : "application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
                response.setContentLengthLong(file.length());
                
                try (FileInputStream inStream = new FileInputStream(file);
                     OutputStream outStream = response.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Documento no encontrado");
            }
            
            rs.close();
            ps.close();
            conexion.cerrarConexion();
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al descargar documento");
        }
    }

    private void eliminarDocumento(HttpServletRequest request, PrintWriter out) throws IOException {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            String idParam = request.getParameter("id");
            int iddocumento = Integer.parseInt(idParam);
            
            CConexion conexion = new CConexion();
            Connection conn = conexion.estableceConexion();
            
            String sqlSelect = "SELECT ruta_archivo FROM documento_cliente WHERE iddocumento = ?";
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            psSelect.setInt(1, iddocumento);
            ResultSet rs = psSelect.executeQuery();
            
            if (rs.next()) {
                String rutaArchivo = rs.getString("ruta_archivo");
                
                String sqlDelete = "DELETE FROM documento_cliente WHERE iddocumento = ?";
                PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
                psDelete.setInt(1, iddocumento);
                
                if (psDelete.executeUpdate() > 0) {
                    String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
                    File file = new File(uploadPath + File.separator + rutaArchivo);
                    if (file.exists()) file.delete();
                    
                    resultado.put("success", true);
                    resultado.put("message", "Documento eliminado exitosamente");
                } else {
                    resultado.put("success", false);
                    resultado.put("message", "Documento no encontrado");
                }
                psDelete.close();
            } else {
                resultado.put("success", false);
                resultado.put("message", "Documento no encontrado");
            }
            
            rs.close();
            psSelect.close();
            conexion.cerrarConexion();
        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al eliminar documento: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.print(gson.toJson(resultado));
    }

    private String getSubmittedFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }
}

