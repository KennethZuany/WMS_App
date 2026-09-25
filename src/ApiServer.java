import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.net.URLDecoder;

/**
 * ApiServer — Lightweight REST API for Zuany WMS.
 * 
 * Uses the JDK built-in HTTP server so the only external dependency
 * remains the PostgreSQL JDBC driver already in lib/.
 * 
 * Endpoints:
 *   GET  /api/productos              → list products
 *   POST /api/productos              → create product
 *   PUT  /api/productos              → update product description
 *   DELETE /api/productos?sku=XXX    → delete product
 * 
 *   GET  /api/categorias             → list categories
 * 
 *   GET  /api/ubicaciones            → list locations
 *   POST /api/ubicaciones            → create location
 *   PUT  /api/ubicaciones            → update location storage type
 *   DELETE /api/ubicaciones?codigo=X → delete location
 * 
 *   GET  /api/empleados              → list employees
 *   POST /api/movimientos/recepcion  → register stock reception
 *   POST /api/movimientos/conteo     → register cycle count
 *   GET  /api/movimientos            → list transactions
 * 
 *   GET  /api/reportes/stock         → stock report
 *   GET  /api/reportes/ocupacion     → warehouse occupancy by stratum
 *   GET  /api/reportes/auditoria     → integral audit
 * 
 *   GET  /*                          → serves static files from web/
 */
public class ApiServer {

    // ── DB credentials (same as App.java) ────────────────────────────
    private static final String DB_URL =
        "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DB_USER = "postgres.bncaznqgzaxexzescpwx";
    private static final String DB_PASS = "Culhuacan8325!";

    private static Connection conexion;

    // ── Entry point ──────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        // 1. Connect to DB
        System.out.println("Conectando a Supabase...");
        conexion = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        System.out.println("¡Éxito! Conexión establecida.");

        // 2. Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // API routes
        server.createContext("/api/productos",             new ProductosHandler());
        server.createContext("/api/categorias",            new CategoriasHandler());
        server.createContext("/api/ubicaciones",           new UbicacionesHandler());
        server.createContext("/api/empleados",             new EmpleadosHandler());
        server.createContext("/api/movimientos/recepcion", new RecepcionHandler());
        server.createContext("/api/movimientos/conteo",    new ConteoHandler());
        server.createContext("/api/movimientos",           new MovimientosHandler());
        server.createContext("/api/reportes/stock",        new ReporteStockHandler());
        server.createContext("/api/reportes/ocupacion",    new OcupacionHandler());
        server.createContext("/api/reportes/auditoria",    new AuditoriaHandler());

        // Static file serving
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("══════════════════════════════════════════════");
        System.out.println("  Zuany WMS — Servidor iniciado en:");
        System.out.println("  http://localhost:8080");
        System.out.println("══════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /** Send a JSON response with a given status code. */
    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /** Read the request body as a String. */
    private static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toString("UTF-8");
    }

    /** Minimal JSON string escaping. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Parse very simple flat JSON { "key":"value", ... } into a Map. */
    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);
        // Tokenize by splitting on commas that are outside quotes
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        List<String> tokens = new ArrayList<>();
        for (char c : json.toCharArray()) {
            if (escaped) { current.append(c); escaped = false; continue; }
            if (c == '\\') { current.append(c); escaped = true; continue; }
            if (c == '"') { inQuotes = !inQuotes; current.append(c); continue; }
            if (c == ',' && !inQuotes) { tokens.add(current.toString()); current = new StringBuilder(); continue; }
            current.append(c);
        }
        if (current.length() > 0) tokens.add(current.toString());

        for (String token : tokens) {
            int colon = token.indexOf(':');
            if (colon < 0) continue;
            String key = token.substring(0, colon).trim();
            String val = token.substring(colon + 1).trim();
            if (key.startsWith("\"") && key.endsWith("\"")) key = key.substring(1, key.length() - 1);
            if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
            // Unescape value
            val = val.replace("\\\"", "\"").replace("\\\\", "\\");
            map.put(key, val);
        }
        return map;
    }

    /** Parse query string parameters */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            try {
                String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                String val = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                map.put(key, val);
            } catch (Exception ignored) {}
        }
        return map;
    }

    /** Handle CORS pre-flight and return true if it was an OPTIONS request. */
    private static boolean handleCors(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════

    static class ProductosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                switch (ex.getRequestMethod()) {
                    case "GET": listProductos(ex); break;
                    case "POST": createProducto(ex); break;
                    case "PUT": updateProducto(ex); break;
                    case "DELETE": deleteProducto(ex); break;
                    default: sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        private void listProductos(HttpExchange ex) throws Exception {
            Statement stmt = conexion.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id_productos, sku_interno, descripcion FROM tb_sys_productos ORDER BY id_productos ASC;");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(rs.getInt("id_productos"))
                  .append(",\"sku\":\"").append(esc(rs.getString("sku_interno")))
                  .append("\",\"descripcion\":\"").append(esc(rs.getString("descripcion")))
                  .append("\"}");
            }
            sb.append("]");
            rs.close(); stmt.close();
            sendJson(ex, 200, sb.toString());
        }

        private void createProducto(HttpExchange ex) throws Exception {
            Map<String, String> data = parseJson(readBody(ex));
            String sql = "INSERT INTO tb_sys_productos (sku_interno, descripcion, sku_fabricante, id_categoria) VALUES (?, ?, ?, ?);";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, data.getOrDefault("sku", "").toUpperCase());
            ps.setString(2, data.getOrDefault("descripcion", ""));
            ps.setString(3, data.getOrDefault("sku_fabricante", ""));
            ps.setInt(4, Integer.parseInt(data.getOrDefault("id_categoria", "0")));
            int rows = ps.executeUpdate();
            ps.close();
            sendJson(ex, rows > 0 ? 201 : 400,
                rows > 0 ? "{\"ok\":true,\"message\":\"Producto registrado\"}"
                         : "{\"ok\":false,\"message\":\"No se pudo registrar\"}");
        }

        private void updateProducto(HttpExchange ex) throws Exception {
            Map<String, String> data = parseJson(readBody(ex));
            String sql = "UPDATE tb_sys_productos SET descripcion = ? WHERE sku_interno = ?;";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, data.getOrDefault("descripcion", ""));
            ps.setString(2, data.getOrDefault("sku", "").toUpperCase());
            int rows = ps.executeUpdate();
            ps.close();
            sendJson(ex, 200,
                rows > 0 ? "{\"ok\":true,\"message\":\"Descripción actualizada\"}"
                         : "{\"ok\":false,\"message\":\"SKU no encontrado\"}");
        }

        private void deleteProducto(HttpExchange ex) throws Exception {
            Map<String, String> q = parseQuery(ex.getRequestURI().getQuery());
            String sku = q.getOrDefault("sku", "").toUpperCase();
            String sql = "DELETE FROM tb_sys_productos WHERE sku_interno = ?;";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, sku);
            int rows = ps.executeUpdate();
            ps.close();
            sendJson(ex, 200,
                rows > 0 ? "{\"ok\":true,\"message\":\"Producto eliminado\"}"
                         : "{\"ok\":false,\"message\":\"SKU no encontrado\"}");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CATEGORIAS
    // ═══════════════════════════════════════════════════════════════════

    static class CategoriasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT id_categoria, nombre_categoria FROM tb_sys_categorias ORDER BY id_categoria ASC;");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"id\":").append(rs.getInt("id_categoria"))
                      .append(",\"nombre\":\"").append(esc(rs.getString("nombre_categoria")))
                      .append("\"}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UBICACIONES
    // ═══════════════════════════════════════════════════════════════════

    static class UbicacionesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                switch (ex.getRequestMethod()) {
                    case "GET": listUbicaciones(ex); break;
                    case "POST": createUbicacion(ex); break;
                    case "PUT": updateUbicacion(ex); break;
                    case "DELETE": deleteUbicacion(ex); break;
                    default: sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        private void listUbicaciones(HttpExchange ex) throws Exception {
            Statement stmt = conexion.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id_ubicacion, codigo_escaneo, estrato, pasillo, bahia, lado, tipo_almacenaje, nivel, consecutivo " +
                "FROM tb_wms_ubicaciones ORDER BY id_ubicacion ASC;");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(rs.getInt("id_ubicacion"))
                  .append(",\"codigo\":\"").append(esc(rs.getString("codigo_escaneo")))
                  .append("\",\"estrato\":\"").append(esc(rs.getString("estrato")))
                  .append("\",\"pasillo\":\"").append(esc(rs.getString("pasillo")))
                  .append("\",\"bahia\":\"").append(esc(rs.getString("bahia")))
                  .append("\",\"lado\":\"").append(esc(rs.getString("lado")))
                  .append("\",\"tipo_almacenaje\":\"").append(esc(rs.getString("tipo_almacenaje")))
                  .append("\",\"nivel\":\"").append(esc(rs.getString("nivel")))
                  .append("\",\"consecutivo\":\"").append(esc(rs.getString("consecutivo")))
                  .append("\"}");
            }
            sb.append("]");
            rs.close(); stmt.close();
            sendJson(ex, 200, sb.toString());
        }

        private void createUbicacion(HttpExchange ex) throws Exception {
            Map<String, String> data = parseJson(readBody(ex));
            String sql = "SELECT sp_registras_ubicacion(?::CHAR, ?::VARCHAR, ?::VARCHAR, ?::CHAR, ?::CHAR, ?::SMALLINT, ?::SMALLINT);";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, data.getOrDefault("estrato", "").toUpperCase());
            ps.setString(2, data.getOrDefault("pasillo", ""));
            ps.setString(3, data.getOrDefault("bahia", ""));
            ps.setString(4, data.getOrDefault("lado", "").toUpperCase());
            ps.setString(5, data.getOrDefault("tipo_almacenaje", "").toUpperCase());
            ps.setInt(6, Integer.parseInt(data.getOrDefault("nivel", "0")));
            ps.setInt(7, Integer.parseInt(data.getOrDefault("consecutivo", "0")));
            ResultSet rs = ps.executeQuery();
            String msg = "";
            if (rs.next()) msg = rs.getString(1);
            rs.close(); ps.close();
            sendJson(ex, 200, "{\"ok\":true,\"message\":\"" + esc(msg) + "\"}");
        }

        private void updateUbicacion(HttpExchange ex) throws Exception {
            Map<String, String> data = parseJson(readBody(ex));
            String sql = "UPDATE tb_wms_ubicaciones SET tipo_almacenaje = ? WHERE codigo_escaneo = ?;";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, data.getOrDefault("tipo_almacenaje", "").toUpperCase());
            ps.setString(2, data.getOrDefault("codigo", "").toUpperCase());
            int rows = ps.executeUpdate();
            ps.close();
            sendJson(ex, 200,
                rows > 0 ? "{\"ok\":true,\"message\":\"Ubicación actualizada\"}"
                         : "{\"ok\":false,\"message\":\"Ubicación no encontrada\"}");
        }

        private void deleteUbicacion(HttpExchange ex) throws Exception {
            Map<String, String> q = parseQuery(ex.getRequestURI().getQuery());
            String codigo = q.getOrDefault("codigo", "").toUpperCase();
            String sql = "DELETE FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;";
            PreparedStatement ps = conexion.prepareStatement(sql);
            ps.setString(1, codigo);
            int rows = ps.executeUpdate();
            ps.close();
            sendJson(ex, 200,
                rows > 0 ? "{\"ok\":true,\"message\":\"Ubicación eliminada\"}"
                         : "{\"ok\":false,\"message\":\"Ubicación no encontrada\"}");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  EMPLEADOS
    // ═══════════════════════════════════════════════════════════════════

    static class EmpleadosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT id_usuario, nombres, apellido_paterno, apellido_materno FROM tb_usr_empleados ORDER BY id_usuario ASC;");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"id\":").append(rs.getInt("id_usuario"))
                      .append(",\"nombres\":\"").append(esc(rs.getString("nombres")))
                      .append("\",\"apellido_paterno\":\"").append(esc(rs.getString("apellido_paterno")))
                      .append("\",\"apellido_materno\":\"").append(esc(rs.getString("apellido_materno")))
                      .append("\"}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOVIMIENTOS — Recepción
    // ═══════════════════════════════════════════════════════════════════

    static class RecepcionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}"); return;
            }
            try {
                Map<String, String> data = parseJson(readBody(ex));
                int operador = Integer.parseInt(data.getOrDefault("operador", "0"));
                String skuProducto = data.getOrDefault("sku", "").toUpperCase();
                String ubicacion = data.getOrDefault("ubicacion", "").toUpperCase();
                int cantidad = Integer.parseInt(data.getOrDefault("cantidad", "0"));
                String condicion = data.getOrDefault("condicion", "A").toUpperCase();

                // Get product id
                PreparedStatement psSku = conexion.prepareStatement("SELECT id_productos FROM tb_sys_productos WHERE sku_interno = ?;");
                psSku.setString(1, skuProducto);
                ResultSet rs1 = psSku.executeQuery();
                int idProductos = 0;
                if (rs1.next()) idProductos = rs1.getInt("id_productos");
                rs1.close(); psSku.close();

                if (idProductos == 0) {
                    sendJson(ex, 400, "{\"ok\":false,\"message\":\"SKU no encontrado\"}"); return;
                }

                // Get location id
                PreparedStatement psUbi = conexion.prepareStatement("SELECT id_ubicacion FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;");
                psUbi.setString(1, ubicacion);
                ResultSet rs2 = psUbi.executeQuery();
                int idUbicacion = 0;
                if (rs2.next()) idUbicacion = rs2.getInt("id_ubicacion");
                rs2.close(); psUbi.close();

                if (idUbicacion == 0) {
                    sendJson(ex, 400, "{\"ok\":false,\"message\":\"Ubicación no encontrada\"}"); return;
                }

                // Check if inventory record exists
                PreparedStatement psSearch = conexion.prepareStatement(
                    "SELECT id_inventario FROM tb_wms_inventario WHERE id_productos = ? AND id_ubicacion = ? AND codigo_condicion = ?;");
                psSearch.setInt(1, idProductos);
                psSearch.setInt(2, idUbicacion);
                psSearch.setString(3, condicion);
                ResultSet rs3 = psSearch.executeQuery();
                int idInventario = 0;

                if (rs3.next()) {
                    idInventario = rs3.getInt("id_inventario");
                    PreparedStatement psUpd = conexion.prepareStatement(
                        "UPDATE tb_wms_inventario SET cantidad = cantidad + ? WHERE id_inventario = ?;");
                    psUpd.setInt(1, cantidad);
                    psUpd.setInt(2, idInventario);
                    psUpd.executeUpdate();
                    psUpd.close();
                } else {
                    String insertSql = "INSERT INTO tb_wms_inventario (id_productos, id_ubicacion, cantidad, codigo_condicion) VALUES (?, ?, ?, ?);";
                    PreparedStatement psIn = conexion.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    psIn.setInt(1, idProductos);
                    psIn.setInt(2, idUbicacion);
                    psIn.setInt(3, cantidad);
                    psIn.setString(4, condicion);
                    psIn.executeUpdate();
                    ResultSet keys = psIn.getGeneratedKeys();
                    if (keys.next()) idInventario = keys.getInt(1);
                    psIn.close();
                }
                rs3.close(); psSearch.close();

                // Log transaction
                PreparedStatement psLog = conexion.prepareStatement(
                    "INSERT INTO tb_log_transacciones (id_usuario, id_inventario, accion, cantidad_afectada) VALUES (?, ?, 'RECEPCION', ?);");
                psLog.setInt(1, operador);
                psLog.setInt(2, idInventario);
                psLog.setInt(3, cantidad);
                psLog.executeUpdate();
                psLog.close();

                sendJson(ex, 200, "{\"ok\":true,\"message\":\"Recepción registrada exitosamente\"}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOVIMIENTOS — Conteo cíclico
    // ═══════════════════════════════════════════════════════════════════

    static class ConteoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}"); return;
            }
            try {
                Map<String, String> data = parseJson(readBody(ex));
                int operador = Integer.parseInt(data.getOrDefault("operador", "0"));
                String skuProducto = data.getOrDefault("sku", "").toUpperCase();
                String ubicacion = data.getOrDefault("ubicacion", "").toUpperCase();
                int stockFisico = Integer.parseInt(data.getOrDefault("cantidad_fisica", "0"));

                // Get product id
                PreparedStatement psSku = conexion.prepareStatement("SELECT id_productos FROM tb_sys_productos WHERE sku_interno = ?;");
                psSku.setString(1, skuProducto);
                ResultSet rs1 = psSku.executeQuery();
                int idProductos = 0;
                if (rs1.next()) idProductos = rs1.getInt("id_productos");
                rs1.close(); psSku.close();

                // Get location id
                PreparedStatement psUbi = conexion.prepareStatement("SELECT id_ubicacion FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;");
                psUbi.setString(1, ubicacion);
                ResultSet rs2 = psUbi.executeQuery();
                int idUbicacion = 0;
                if (rs2.next()) idUbicacion = rs2.getInt("id_ubicacion");
                rs2.close(); psUbi.close();

                // Get current stock
                PreparedStatement psInv = conexion.prepareStatement(
                    "SELECT id_inventario, cantidad FROM tb_wms_inventario WHERE id_productos = ? AND id_ubicacion = ?;");
                psInv.setInt(1, idProductos);
                psInv.setInt(2, idUbicacion);
                ResultSet rs3 = psInv.executeQuery();

                if (rs3.next()) {
                    int idInventario = rs3.getInt("id_inventario");
                    int stockSistema = rs3.getInt("cantidad");

                    if (stockSistema == stockFisico) {
                        sendJson(ex, 200, "{\"ok\":true,\"message\":\"Conteo correcto. Sin diferencias.\",\"diferencia\":0}");
                    } else {
                        int diferencia = stockFisico - stockSistema;
                        PreparedStatement psUpd = conexion.prepareStatement(
                            "UPDATE tb_wms_inventario SET cantidad = ? WHERE id_inventario = ?;");
                        psUpd.setInt(1, stockFisico);
                        psUpd.setInt(2, idInventario);
                        psUpd.executeUpdate();
                        psUpd.close();

                        PreparedStatement psLog = conexion.prepareStatement(
                            "INSERT INTO tb_log_transacciones (id_usuario, id_inventario, accion, cantidad_afectada) VALUES (?, ?, 'AJUSTE', ?);");
                        psLog.setInt(1, operador);
                        psLog.setInt(2, idInventario);
                        psLog.setInt(3, diferencia);
                        psLog.executeUpdate();
                        psLog.close();

                        sendJson(ex, 200, "{\"ok\":true,\"message\":\"Diferencia encontrada. Cantidad actualizada.\",\"diferencia\":" + diferencia + "}");
                    }
                } else {
                    sendJson(ex, 400, "{\"ok\":false,\"message\":\"No existe el producto en la ubicación indicada\"}");
                }
                rs3.close(); psInv.close();
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOVIMIENTOS — Historial
    // ═══════════════════════════════════════════════════════════════════

    static class MovimientosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT id_transaccion, id_usuario, id_inventario, accion, cantidad_afectada, timestamp " +
                    "FROM tb_log_transacciones ORDER BY id_transaccion DESC;");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"id\":").append(rs.getInt("id_transaccion"))
                      .append(",\"id_usuario\":").append(rs.getInt("id_usuario"))
                      .append(",\"id_inventario\":").append(rs.getInt("id_inventario"))
                      .append(",\"accion\":\"").append(esc(rs.getString("accion")))
                      .append("\",\"cantidad\":").append(rs.getInt("cantidad_afectada"))
                      .append(",\"fecha\":\"").append(esc(rs.getString("timestamp")))
                      .append("\"}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REPORTES — Stock total
    // ═══════════════════════════════════════════════════════════════════

    static class ReporteStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM sp_reporte_stock();");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"id\":").append(rs.getInt("id_productos"))
                      .append(",\"sku\":\"").append(esc(rs.getString("sku_interno")))
                      .append("\",\"descripcion\":\"").append(esc(rs.getString("descripcion")))
                      .append("\",\"stock\":").append(rs.getLong("stock_total"))
                      .append("}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REPORTES — Ocupación por estrato
    // ═══════════════════════════════════════════════════════════════════

    static class OcupacionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT u.estrato, COUNT(u.id_ubicacion) AS total_ubicaciones, COALESCE(SUM(i.cantidad), 0) AS total_piezas " +
                    "FROM tb_wms_ubicaciones u LEFT JOIN tb_wms_inventario i ON u.id_ubicacion = i.id_ubicacion " +
                    "GROUP BY u.estrato ORDER BY u.estrato ASC;");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"estrato\":\"").append(esc(rs.getString("estrato")))
                      .append("\",\"ubicaciones\":").append(rs.getInt("total_ubicaciones"))
                      .append(",\"piezas\":").append(rs.getInt("total_piezas"))
                      .append("}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REPORTES — Auditoría
    // ═══════════════════════════════════════════════════════════════════

    static class AuditoriaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            try {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT '📝 NORMAL' AS tipo_evento, accion, cantidad_afectada " +
                    "FROM tb_log_transacciones WHERE accion != 'AJUSTE' " +
                    "UNION ALL " +
                    "SELECT '⚠️ ALERTA', 'AJUSTE/DEVOLUCION', cantidad_afectada " +
                    "FROM tb_wms_alertas_ajustes ORDER BY cantidad_afectada DESC;");
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"evento\":\"").append(esc(rs.getString("tipo_evento")))
                      .append("\",\"accion\":\"").append(esc(rs.getString("accion")))
                      .append("\",\"cantidad\":").append(rs.getInt("cantidad_afectada"))
                      .append("}");
                }
                sb.append("]");
                rs.close(); stmt.close();
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STATIC FILE SERVER
    // ═══════════════════════════════════════════════════════════════════

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            // Resolve relative to the "web" directory alongside the project root
            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                String notFound = "404 Not Found";
                ex.sendResponseHeaders(404, notFound.length());
                ex.getResponseBody().write(notFound.getBytes());
                ex.getResponseBody().close();
                return;
            }

            // Content type detection
            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js"))  contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (path.endsWith(".ico")) contentType = "image/x-icon";
            else if (path.endsWith(".woff2")) contentType = "font/woff2";
            else if (path.endsWith(".woff")) contentType = "font/woff";

            byte[] bytes = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }
    }
}
