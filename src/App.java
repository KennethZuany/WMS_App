import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class App {
    public static void main(String[] args) throws Exception {
        // 1. Configuración de credenciales de Supabase
        String url = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require";
        String usuario = "postgres.bncaznqgzaxexzescpwx";
        String contrasena = "Culhuacan8325!";

        // 2. Intentar establecer la conexión
        try {
            System.out.println("Conectando a Supabase...");
            Connection conexion = DriverManager.getConnection(url, usuario, contrasena);
            
            System.out.println("¡Éxito! Conexión establecida correctamente.");
            
            Scanner sc = new Scanner(System.in);
            int opcion_principal = 0;

            do{
                System.out.println("======================================================================");
                System.out.println("- - - - - - - - - - Zuany WMS - - - Menú Principal - - - - - - - - - -");
                System.out.println("======================================================================");
                System.out.println("1. 📦 Módulo de Productos");
                System.out.println("2. 🏢 Módulo de Ubicaciones");
                System.out.println("3. 🔄 Módulo de Movimientos");
                System.out.println("4. 📊 Módulo de Reportes Generales");
                System.out.println("5. ❌ Salir del Sistema");
                System.out.println("Elige un módulo: ");

                opcion_principal = Integer.parseInt(sc.nextLine());                
                switch (opcion_principal) {
                    case 1:
                        int opcion_productos = 0;
                        do {
                            System.out.println("======================================================================");
                            System.out.println("- - - - - - - - - - - - 📦 Módulo de Productos - - - - - - - - - - - -");
                            System.out.println("======================================================================");
                            System.out.println("1. 🔍 Consultar catálogo de productos");
                            System.out.println("2. ➕ Dar de alta un nuevo producto");
                            System.out.println("3. ✏️ Modificar la descripción de un producto");
                            System.out.println("4. 🗑️ Dar de baja un producto");
                            System.out.println("5. 🔙 Volver al menú principal");
                            System.out.println("Selecciona una opción: ");
                            
                            opcion_productos = Integer.parseInt(sc.nextLine());
                            switch (opcion_productos) {
                                case 1:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - 🔍 Consultar catálogo de productos - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        // Leemos directo de la tabla de catálogo, ordenados por ID
                                        ResultSet rs = stmt.executeQuery("SELECT id_productos, sku_interno, descripcion FROM tb_sys_productos ORDER BY id_productos ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-5s | %-15s | %-30s\n", "ID", "SKU", "Descripción");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id = rs.getInt("id_productos");
                                            String sku = rs.getString("sku_interno");
                                            String descripcion = rs.getString("descripcion");
                                            
                                            if (descripcion.length() > 27) {
                                                descripcion = descripcion.substring(0, 27) + "...";
                                            }
                                            System.out.printf("%-5d | %-15s | %-30s\n", id, sku, descripcion);
                                        }
                                        rs.close();
                                        stmt.close();
                                    } catch (SQLException e) {
                                        System.out.println("Error al consultar el catálogo: " + e.getMessage());
                                    }
                                    break;
                                    
                                case 2:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - ➕ Dar de alta un nuevo producto - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        System.out.print("Ingresa el SKU (Ej. SYS-NUEVO-001): ");
                                        String sku = sc.nextLine().toUpperCase(); // Forzamos mayúsculas por limpieza
                                        
                                        System.out.print("Ingresa la descripción del producto: ");
                                        String descripcion = sc.nextLine();

                                        System.out.print("Ingresa el SKU del fabricante: ");
                                        String sku_fabricante = sc.nextLine();

                                        try {
                                            Statement stmt = conexion.createStatement();
                                            // Leemos directo de la tabla de catálogo, ordenados por ID
                                            ResultSet rs = stmt.executeQuery("SELECT id_categoria, nombre_categoria FROM tb_sys_categorias ORDER BY id_categoria ASC;");
                                        System.out.println();
                                        System.out.printf("%-5s | %-25s\n", "ID", "NOMBRE");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id_categoria = rs.getInt("id_categoria");
                                            String nombre_categoria = rs.getString("nombre_categoria");
                                            
                                            System.out.printf("%-5d | %-25s\n", id_categoria, nombre_categoria);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();

                                        } catch (SQLException e) {
                                            System.out.println("Error al consultar las categorias: " + e.getMessage());
                                        }

                                        System.out.print("Ingresa el id de categoría a la que pertenece: ");
                                        int id_categoria = Integer.parseInt(sc.nextLine());
                                        
                                        // Inyectamos directo a la tabla (sin función en Supabase)
                                        String queryInsert = "INSERT INTO tb_sys_productos (sku_interno, descripcion, sku_fabricante, id_categoria) VALUES (?, ?, ?, ?);";
                                        PreparedStatement pstmtIn = conexion.prepareStatement(queryInsert);
                                        pstmtIn.setString(1, sku);
                                        pstmtIn.setString(2, descripcion);
                                        pstmtIn.setString(3, sku_fabricante);
                                        pstmtIn.setInt(4, id_categoria);
                                        
                                        int filasInsertadas = pstmtIn.executeUpdate();
                                        if (filasInsertadas > 0) {
                                            System.out.println("\n¡Éxito! Producto registrado en el catálogo.");
                                        }
                                        pstmtIn.close();
                                    } catch (SQLException e) {
                                        System.out.println("\nError al registrar producto: " + e.getMessage());
                                    }
                                    break;
                                    
                                case 3:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - ✏️ Modificar la descripción de un producto - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        System.out.print("Ingresa el SKU exacto del producto a modificar: ");
                                        String skuUpdate = sc.nextLine().toUpperCase();
                                                
                                        System.out.print("Ingresa la NUEVA descripción: ");
                                        String nuevaDesc = sc.nextLine();
                                        
                                        String queryUpdate = "UPDATE tb_sys_productos SET descripcion = ? WHERE sku_interno = ?;";
                                        PreparedStatement pstmtUp = conexion.prepareStatement(queryUpdate);
                                        pstmtUp.setString(1, nuevaDesc);
                                        pstmtUp.setString(2, skuUpdate);
                                            
                                        // executeUpdate() devuelve el número de filas que fueron afectadas
                                        int filasActualizadas = pstmtUp.executeUpdate();
                                        if (filasActualizadas > 0) {
                                            System.out.println("\n¡Éxito! Descripción actualizada correctamente.");
                                        } else {
                                            System.out.println("\nError: No se encontró ningún producto con el SKU " + skuUpdate);
                                        }
                                        pstmtUp.close();
                                    } catch (SQLException e) {
                                        System.out.println("\nError al modificar producto: " + e.getMessage());
                                    }
                                    break;
                                            
                                case 4:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - - 🗑️ Dar de baja un producto - - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        System.out.print("Ingresa el SKU exacto del producto a eliminar: ");
                                        String skuDelete = sc.nextLine().toUpperCase();
                                                    
                                        // Advertencia de seguridad básica
                                        System.out.print("¿Estás seguro de eliminar " + skuDelete + "? (S/N): ");
                                        String confirmacion = sc.nextLine().toUpperCase();
                                                    
                                        if (confirmacion.equals("S")) {
                                            String queryDelete = "DELETE FROM tb_sys_productos WHERE sku_interno = ?;";
                                            PreparedStatement pstmtDel = conexion.prepareStatement(queryDelete);
                                            pstmtDel.setString(1, skuDelete);
                                                        
                                            int filasBorradas = pstmtDel.executeUpdate();
                                            if (filasBorradas > 0) {
                                                System.out.println("\n¡Éxito! Producto eliminado del sistema.");
                                            } else {
                                                System.out.println("\nError: No se encontró ningún producto con el SKU " + skuDelete);
                                            }
                                            pstmtDel.close();
                                        } else {
                                            System.out.println("\nOperación cancelada.");
                                        }
                                    } catch (SQLException e) {
                                        // Aquí brincará el error si intentas borrar un producto que tiene inventario ligado
                                        System.out.println("\nError de base de datos: " + e.getMessage());
                                    }
                                    break;
                                                         
                                case 5:
                                    System.out.println("Volviendo al Menú Principal...");
                                    break;
                            
                                default:
                                    System.out.println("Opción invalida. Intente con una opción correcta.");
                                    break;
                            }
                        } while (opcion_productos != 5);

                        break;
                        
                    case 2:
                        int opcion_ubicaciones = 0;
                        do {
                            System.out.println("======================================================================");
                            System.out.println("- - - - - - - - - - - - 🏢 Módulo de Ubicaciones - - - - - - - - - - -");
                            System.out.println("======================================================================");
                            System.out.println("1. 📍 Consultar ubicaciones existentes");
                            System.out.println("2. 📌 Registrar nueva ubicación");
                            System.out.println("3. 🔧 Modificar tipo de almacenaje");
                            System.out.println("4. 🚧 Eliminar ubicación");
                            System.out.println("5. 🔙 Volver al menú principal");
                            System.out.println("Selecciona una opción: ");
                            
                            opcion_ubicaciones = Integer.parseInt(sc.nextLine());
                            switch (opcion_ubicaciones) {
                                case 1:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - 📍 Consultar ubicaciones existentes - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        // Leemos directo de la tabla de catálogo, ordenados por ID
                                        ResultSet rs = stmt.executeQuery("SELECT id_ubicacion, codigo_escaneo, estrato, pasillo, bahia, lado, tipo_almacenaje, nivel, consecutivo FROM tb_wms_ubicaciones ORDER BY id_ubicacion ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-5s | %-15s | %-8s | %-8s | %-6s | %-5s | %-16s | %-6s | %-12s\n", "ID", "UBICACIÓN", "ESTRATO", "PASILLO", "BAHÍA", "LADO", "TIPO ALMACENAJE", "NIVEL", "CONSECUTIVO");
                                        System.out.println("---------------------------------------------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id_ubicacion = rs.getInt("id_ubicacion");
                                            String codigo_escaneo = rs.getString("codigo_escaneo");
                                            String estrato = rs.getString("estrato");
                                            String pasillo = rs.getString("pasillo");
                                            String bahia = rs.getString("bahia");
                                            String lado = rs.getString("lado");
                                            String tipo_almacenaje = rs.getString("tipo_almacenaje");
                                            String nivel  = rs.getString("nivel");
                                            String consecutivo = rs.getString("consecutivo");
                                            
                                            System.out.printf("%-5s | %-15s | %-8s | %-8s | %-6s | %-5s | %-16s | %-6s | %-12s\n", id_ubicacion, codigo_escaneo, estrato, pasillo, bahia, lado, tipo_almacenaje, nivel, consecutivo);
                                        }
                                        System.out.println("---------------------------------------------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();
                                    } catch (SQLException e) {
                                        System.out.println("Error al consultar las ubicaciones: " + e.getMessage());
                                    }
                                    break;

                                case 2:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - - 📌 Registrar nueva ubicación - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        // 1. Pedir los datos al usuario
                                        System.out.print("Estrato (Ej. A): ");
                                        String estrato = sc.nextLine();
                                        
                                        System.out.print("Pasillo (Ej. 01): ");
                                        String pasillo = sc.nextLine();
                                        
                                        System.out.print("Bahía (Ej. 05): ");
                                        String bahia = sc.nextLine();
                                        
                                        System.out.print("Lado (Ej. I): ");
                                        String lado = sc.nextLine();
                                        
                                        System.out.print("Tipo de Almacenaje (Ej. A): ");
                                        String tipo_almacenaje = sc.nextLine();
                                        
                                        System.out.print("Nivel (Ej. 2): ");
                                        int nivel = Integer.parseInt(sc.nextLine());
                                        
                                        System.out.print("Consecutivo (Ej. 1): ");
                                        int consecutivo = Integer.parseInt(sc.nextLine());
                                        
                                        System.out.println();
                                        
                                        // 2. Preparar la consulta con huecos (?) para los 7 parámetros
                                        String query = "SELECT sp_registras_ubicacion(?::CHAR, ?::VARCHAR, ?::VARCHAR, ?::CHAR, ?::CHAR, ?::SMALLINT, ?::SMALLINT);";
                                        PreparedStatement pstmt = conexion.prepareStatement(query);
                                        
                                        // 3. Rellenar los huecos en orden (1 al 7) con las variables capturadas
                                        pstmt.setString(1, estrato.toUpperCase());
                                        pstmt.setString(2, pasillo);
                                        pstmt.setString(3, bahia);
                                        pstmt.setString(4, lado.toUpperCase());
                                        pstmt.setString(5, tipo_almacenaje.toUpperCase());
                                        pstmt.setInt(6, nivel);
                                        pstmt.setInt(7, consecutivo);
                                        
                                        // 4. Ejecutar y atrapar el mensaje de texto que retorna tu función (el de Éxito o el de Error)
                                        ResultSet rs = pstmt.executeQuery();
                                        if (rs.next()) {
                                            // Imprimimos la columna 1, que contiene el mensaje que configuraste en tu bloque EXCEPTION en SQL
                                            System.out.println("\nRespuesta del servidor: " + rs.getString(1));
                                        }
                                        
                                        rs.close();
                                        pstmt.close();
                                        
                                    } catch (SQLException e) {
                                        System.out.println("\nError de base de datos: " + e.getMessage());
                                    } catch (NumberFormatException e) {
                                        System.out.println("\nError: Debes ingresar un número válido para el nivel o consecutivo.");
                                    }
                                    break;        

                                case 3:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - 🔧 Modificar tipo de almacenaje - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        System.out.print("Ingresa la ubicación a modificar: ");
                                        String ubicacion_update = sc.nextLine().toUpperCase();
                                                
                                        System.out.print("Ingresa el nuevo tipo de almacenaje: ");
                                        String nuevo_tipo_almacenaje = sc.nextLine().toUpperCase();
                                        
                                        String queryUpdate = "UPDATE tb_wms_ubicaciones SET tipo_almacenaje = ? WHERE codigo_escaneo = ?;";
                                        PreparedStatement pstmtUp = conexion.prepareStatement(queryUpdate);
                                        pstmtUp.setString(1, nuevo_tipo_almacenaje);
                                        pstmtUp.setString(2, ubicacion_update);
                                            
                                        // executeUpdate() devuelve el número de filas que fueron afectadas
                                        int filasActualizadas = pstmtUp.executeUpdate();
                                        if (filasActualizadas > 0) {
                                            System.out.println("\n¡Éxito! Ubicación actualizada correctamente.");
                                        } else {
                                            System.out.println("\nError: No se encontró ningúna ubicación: " + ubicacion_update);
                                        }
                                        pstmtUp.close();
                                    } catch (SQLException e) {
                                        System.out.println("\nError al modificar ubicación: " + e.getMessage());
                                    }
                                    break;

                                case 4:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - - - - 🚧 Eliminar ubicación - - - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        System.out.print("Ingresa la ubicación a eliminar: ");
                                        String ubi_delete = sc.nextLine().toUpperCase();
                                                    
                                        // Advertencia de seguridad básica
                                        System.out.print("¿Estás seguro de eliminar " + ubi_delete + "? (S/N): ");
                                        String confirmacion = sc.nextLine().toUpperCase();
                                                    
                                        if (confirmacion.equals("S")) {
                                            String queryDelete = "DELETE FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;";
                                            PreparedStatement pstmtDel = conexion.prepareStatement(queryDelete);
                                            pstmtDel.setString(1, ubi_delete);
                                                        
                                            int filasBorradas = pstmtDel.executeUpdate();
                                            if (filasBorradas > 0) {
                                                System.out.println("\n¡Éxito! Ubicacion eliminada del sistema.");
                                            } else {
                                                System.out.println("\nError: No se encontró ninguna ubicación: " + ubi_delete);
                                            }
                                            pstmtDel.close();
                                        } else {
                                            System.out.println("\nOperación cancelada.");
                                        }
                                    } catch (SQLException e) {
                                        System.out.println("\nError de base de datos: " + e.getMessage());
                                    }
                                    break;

                                case 5:
                                    System.out.println("Volviendo al Menú Principal...");
                                    break;
                            
                                default:
                                    System.out.println("Opción invalida. Intente con una opción correcta.");
                                    break;
                            }
                        } while (opcion_ubicaciones != 5);

                        break;
                    
                    case 3:
                        int opcion_movimientos = 0;
                        do {
                            System.out.println("======================================================================");
                            System.out.println("- - - - - - - - - - - - 🔄 Módulo de Movimientos - - - - - - - - - - -");
                            System.out.println("======================================================================");
                            System.out.println("1. 🚚 Registrar recepción de stock");
                            System.out.println("2. 📋 Registrar asignación o conteo cíclico");
                            System.out.println("3. 🕒 Consultar últimos movimientos");
                            System.out.println("4. 🔙 Volver al menú principal");
                            System.out.println("Selecciona una opción: ");

                            opcion_movimientos = Integer.parseInt(sc.nextLine());
                            switch (opcion_movimientos) {
                                case 1:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - 🚚 Registrar recepción de stock - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        // 1. Pedir los datos al usuario
                                        Statement stmt = conexion.createStatement();
                                        ResultSet rs = stmt.executeQuery("SELECT id_usuario, nombres, apellido_paterno, apellido_materno FROM tb_usr_empleados ORDER BY id_usuario ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-5s | %-20s | %-20s | %-20s\n", "ID", "NOMBRES", "APELLIDO PATERNO", "APELLIDO MATERNO");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id = rs.getInt("id_usuario");
                                            String nombres = rs.getString("nombres");
                                            String apellido_paterno = rs.getString("apellido_paterno");
                                            String apellido_materno = rs.getString("apellido_materno");
                                            
                                            System.out.printf("%-5s | %-20s | %-20s | %-20s\n", id, nombres, apellido_paterno, apellido_materno);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();

                                        System.out.print("Usuario a cargo del movimiento: (Ej. 3): ");
                                        int operador = Integer.parseInt(sc.nextLine());

                                        System.out.print("SKU del producto (Ej. SYS-RTR-001): ");
                                        String sku_producto = sc.nextLine().toUpperCase();
                                        PreparedStatement pstmtSKU = conexion.prepareStatement("SELECT id_productos FROM tb_sys_productos WHERE sku_interno = ?;");
                                        pstmtSKU.setString(1, sku_producto);
                                        ResultSet rs1 = pstmtSKU.executeQuery();
                                        int id_productos = 0;
                                        if(rs1.next()){
                                            id_productos = rs1.getInt("id_productos");
                                        }
                                        rs1.close();
                                        pstmtSKU.close();

                                        System.out.print("Ubicación de la mercancía (Ej. B0405DR212): ");
                                        String ubicacion = sc.nextLine().toUpperCase();
                                        PreparedStatement pstmtUbi = conexion.prepareStatement("SELECT id_ubicacion FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;");
                                        pstmtUbi.setString(1, ubicacion);
                                        ResultSet rs2 = pstmtUbi.executeQuery();
                                        int id_ubicacion = 0;
                                        if(rs2.next()){
                                            id_ubicacion = rs2.getInt("id_ubicacion");
                                        }
                                        rs2.close();
                                        pstmtUbi.close();

                                        System.out.print("Cantídad del movimiento (Ej. 300): ");
                                        int cantidad = Integer.parseInt(sc.nextLine());
                                        
                                        System.out.print("Condición (Ej. A): ");
                                        String condicion = sc.nextLine().toUpperCase();
                                        
                                        System.out.println();
                                        
                                        PreparedStatement pstmtSearch = conexion.prepareStatement("SELECT id_inventario FROM tb_wms_inventario WHERE id_productos = ? AND id_ubicacion = ? AND codigo_condicion = ?;");
                                        pstmtSearch.setInt(1, id_productos);
                                        pstmtSearch.setInt(2, id_ubicacion);
                                        pstmtSearch.setString(3, condicion);
                                        ResultSet rs3 = pstmtSearch.executeQuery();
                                        int id_inventario = 0;
                                        if(rs3.next()){
                                            id_inventario = rs3.getInt("id_inventario");
                                            PreparedStatement pstmtUpdate = conexion.prepareStatement("UPDATE tb_wms_inventario SET cantidad = cantidad + ? WHERE id_inventario = ?;");
                                            pstmtUpdate.setInt(1, cantidad);
                                            pstmtUpdate.setInt(2, id_inventario);
                                            pstmtUpdate.executeUpdate();
                                            pstmtUpdate.close();
                                        } else {
                                            // 1. Agregamos la bandera RETURN_GENERATED_KEYS al preparar la consulta
                                            String queryInsertInventory = "INSERT INTO tb_wms_inventario (id_productos, id_ubicacion, cantidad, codigo_condicion) VALUES (?, ?, ?, ?);";
                                            PreparedStatement pstmtIn = conexion.prepareStatement(queryInsertInventory, Statement.RETURN_GENERATED_KEYS);
                                            
                                            pstmtIn.setInt(1, id_productos);
                                            pstmtIn.setInt(2, id_ubicacion);
                                            pstmtIn.setInt(3, cantidad);
                                            pstmtIn.setString(4, condicion);
                                            
                                            int filasInsertadas = pstmtIn.executeUpdate();
                                            if (filasInsertadas > 0) {
                                                // 2. Atrapamos el nuevo ID que acaba de nacer y actualizamos la variable
                                                ResultSet rsKeys = pstmtIn.getGeneratedKeys();
                                                if (rsKeys.next()) {
                                                    id_inventario = rsKeys.getInt(1); // ¡Adiós al 0! Ahora tiene el ID real.
                                                }
                                                System.out.println("\n¡Éxito! Movimiento Registrado.");
                                            }
                                            pstmtIn.close();
                                        }
                                        
                                        PreparedStatement pstmtLog = conexion.prepareStatement("INSERT INTO tb_log_transacciones (id_usuario, id_inventario, accion, cantidad_afectada) VALUES (?, ?, 'RECEPCION', ?);");
                                        pstmtLog.setInt(1, operador);
                                        pstmtLog.setInt(2, id_inventario);
                                        pstmtLog.setInt(3, cantidad);
                                        pstmtLog.executeUpdate();
                                        
                                    } catch (SQLException e) {
                                        System.out.println("\nError al registrar recepción: " + e.getMessage());
                                    }

                                    break;

                                case 2:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - 📋 Registrar asignación o conteo cíclico - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        ResultSet rs = stmt.executeQuery("SELECT id_usuario, nombres, apellido_paterno, apellido_materno FROM tb_usr_empleados ORDER BY id_usuario ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-5s | %-20s | %-20s | %-20s\n", "ID", "NOMBRES", "APELLIDO PATERNO", "APELLIDO MATERNO");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id = rs.getInt("id_usuario");
                                            String nombres = rs.getString("nombres");
                                            String apellido_paterno = rs.getString("apellido_paterno");
                                            String apellido_materno = rs.getString("apellido_materno");
                                            
                                            System.out.printf("%-5s | %-20s | %-20s | %-20s\n", id, nombres, apellido_paterno, apellido_materno);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();

                                        System.out.print("Usuario a cargo del movimiento: (Ej. 3): ");
                                        int operador = Integer.parseInt(sc.nextLine());

                                        System.out.print("SKU del producto (Ej. SYS-RTR-001): ");
                                        String sku_producto = sc.nextLine().toUpperCase();
                                        PreparedStatement pstmtSKU = conexion.prepareStatement("SELECT id_productos FROM tb_sys_productos WHERE sku_interno = ?;");
                                        pstmtSKU.setString(1, sku_producto);
                                        ResultSet rs1 = pstmtSKU.executeQuery();
                                        int id_productos = 0;
                                        if(rs1.next()){
                                            id_productos = rs1.getInt("id_productos");
                                        }
                                        rs1.close();
                                        pstmtSKU.close();

                                        System.out.print("Ubicación de la mercancía (Ej. B0405DR212): ");
                                        String ubicacion = sc.nextLine().toUpperCase();
                                        PreparedStatement pstmtUbi = conexion.prepareStatement("SELECT id_ubicacion FROM tb_wms_ubicaciones WHERE codigo_escaneo = ?;");
                                        pstmtUbi.setString(1, ubicacion);
                                        ResultSet rs2 = pstmtUbi.executeQuery();
                                        int id_ubicacion = 0;
                                        if(rs2.next()){
                                            id_ubicacion = rs2.getInt("id_ubicacion");
                                        }
                                        rs2.close();
                                        pstmtUbi.close();

                                        PreparedStatement pstmtInvNow = conexion.prepareStatement("SELECT id_inventario, cantidad FROM tb_wms_inventario WHERE id_productos = ? AND id_ubicacion = ?;");
                                        pstmtInvNow.setInt(1, id_productos);
                                        pstmtInvNow.setInt(2, id_ubicacion);
                                        ResultSet rs3 = pstmtInvNow.executeQuery();
                                        int stock_sistema = 0;
                                        int id_inventario = 0;
                                        if(rs3.next()){
                                            id_inventario = rs3.getInt("id_inventario");
                                            stock_sistema = rs3.getInt("cantidad");
                                            System.out.print("Cantidad física (Ej. 12): ");
                                            int stock_fisico = Integer.parseInt(sc.nextLine());
                                            if(stock_sistema == stock_fisico){
                                                System.out.println("Felicidades. Conteo correcto, no requiere modificaciones.");

                                            } else {
                                                int diferencia = stock_fisico - stock_sistema;
                                                PreparedStatement pstmtUpdInv = conexion.prepareStatement("UPDATE tb_wms_inventario SET cantidad = ? WHERE id_inventario = ?;");
                                                pstmtUpdInv.setInt(1, stock_fisico);
                                                pstmtUpdInv.setInt(2, id_inventario);
                                                int filasActualizadas = pstmtUpdInv.executeUpdate();
                                                if(filasActualizadas > 0){
                                                    System.out.println("Diferencia encontrada. Cantidad actualizada.");
                                                }
                                                PreparedStatement pstmtInsLog = conexion.prepareStatement("INSERT INTO tb_log_transacciones (id_usuario, id_inventario, accion, cantidad_afectada) VALUES (?, ?, 'AJUSTE', ?);");
                                                pstmtInsLog.setInt(1, operador);
                                                pstmtInsLog.setInt(2, id_inventario);
                                                pstmtInsLog.setInt(3, diferencia);
                                                pstmtUpdInv.close();
                                                pstmtInsLog.executeUpdate();
                                                pstmtInsLog.close();

                                            }
                                        
                                        } else {
                                            System.out.println("❌ No existe el producto en la ubicación");
                                        }
                                        rs3.close();
                                        pstmtInvNow.close();
                                        
                                    } catch (SQLException e) {
                                        System.out.println("\nError al registrar asignación: " + e.getMessage());
                                    }

                                    break;

                                case 3:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - - - 🕒 Consultar movimientos - - - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        // Leemos directo de la tabla de catálogo, ordenados por ID
                                        ResultSet rs = stmt.executeQuery("SELECT id_transaccion, id_usuario, id_inventario, accion, cantidad_afectada, timestamp FROM tb_log_transacciones ORDER BY id_transaccion ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-5s | %-8s | %-10s | %-12s | %-6s | %-30s\n", "ID", "USUARIO", "INVENTARIO", "ACCION", "CANTIDAD", "FECHA");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            int id_transaccion = rs.getInt("id_transaccion");
                                            int id_usuario = rs.getInt("id_usuario");
                                            int id_inventario = rs.getInt("id_inventario");
                                            String accion = rs.getString("accion");
                                            int cantidad_afectada = rs.getInt("cantidad_afectada");
                                            String timestamp = rs.getString("timestamp");
                                            
                                            System.out.printf("%-5s | %-8s | %-5s | %-12s | %-6s | %-30s\n", id_transaccion, id_usuario, id_inventario, accion, cantidad_afectada, timestamp);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();
                                    } catch (SQLException e) {
                                        System.out.println("Error al consultar los movimientos: " + e.getMessage());
                                    }
                                    break;

                                case 4:
                                    System.out.println("Volviendo al Menú Principal...");

                                    break;

                                default:
                                    System.out.println("Opción invalida. Intente con una opción correcta.");

                                    break;
                            }
                        } while (opcion_movimientos != 4);
                        break;

                    case 4:
                        int opcion_reportes = 0;
                        do {
                            System.out.println("======================================================================");
                            System.out.println("- - - - - - - - - - - 📊 Módulo de Reportes Generales - - - - - - - - -");
                            System.out.println("======================================================================");
                            System.out.println("1. 📦 Ver reporte de stock total");
                            System.out.println("2. 🏢 Ver ocupación de almacén por estrato");
                            System.out.println("3. 🛡️ Ver auditoría integral de inventario");
                            System.out.println("4. 🔙 Volver al menú principal");
                            System.out.println("Selecciona una opción: ");

                            opcion_reportes = Integer.parseInt(sc.nextLine());
                            switch (opcion_reportes) {
                                case 1:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - - - 📦 Ver reporte de stock total - - - - - - - - - -");
                                    System.out.println("======================================================================");
                                    
                                    try {
                                        // 1. Crear el vehículo para enviar la consulta
                                        Statement stmt = conexion.createStatement(); // vehículo encargado de transportar consulta SQL hacia Supabase.
                                        
                                        
                                        // 2. Ejecutar tu procedimiento (recuerda que en Postgres las funciones se llaman con SELECT)
                                        ResultSet rs = stmt.executeQuery("SELECT * FROM sp_reporte_stock();"); // Devuelve la tabla con los datos
                                        
                                        // 3. Imprimir el encabezado visual de la tabla
                                        System.out.printf("%-5s | %-15s | %-30s | %-10s\n", "ID", "SKU", "Descripción", "Stock");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        // 4. Recorrer la "caja" de resultados fila por fila mientras haya datos
                                        while (rs.next()) {
                                            // Extraemos cada columna usando el nombre exacto que le diste en tu RETURNS TABLE
                                            int id = rs.getInt("id_productos");
                                            String sku = rs.getString("sku_interno");
                                            String descripcion = rs.getString("descripcion");
                                            long stock = rs.getLong("stock_total"); 
                                            if (descripcion.length() > 27) {
                                                descripcion = descripcion.substring(0, 27) + "...";
                                            }
                                            
                                            // Imprimir la fila con las columnas alineadas
                                            System.out.printf("%-5d | %-15s | %-30s | %-10d\n", id, sku, descripcion, stock);
                                        }
                                        
                                        // 5. Cerrar las herramientas de consulta por limpieza
                                        rs.close();
                                        stmt.close();
                                        System.out.println("======================================================================");
                                        
                                    } catch (SQLException e) {
                                        System.out.println("Error al generar el reporte: " + e.getMessage());
                                    }
                                    break;
                                    
                                case 2:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - 🏢 Ver ocupación de almacén por estrato - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        ResultSet rs = stmt.executeQuery("SELECT u.estrato, COUNT(u.id_ubicacion) AS total_ubicaciones, COALESCE(SUM(i.cantidad), 0) AS total_piezas \n" + //
                                                                                        "FROM tb_wms_ubicaciones u \n" + //
                                                                                        "LEFT JOIN tb_wms_inventario i ON u.id_ubicacion = i.id_ubicacion \n" + //
                                                                                        "GROUP BY u.estrato \n" + //
                                                                                        "ORDER BY u.estrato ASC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-8s | %-18s | %-12s\n", "ESTRATO", "TOTAL UBICACIONES", "TOTAL PIEZAS");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            String estrato = rs.getString("estrato");
                                            int total_ubicaciones = rs.getInt("total_ubicaciones");
                                            int total_piezas = rs.getInt("total_piezas");
                                            
                                            System.out.printf("%-8s | %-18s | %-12s\n", estrato, total_ubicaciones, total_piezas);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();
                                    } catch (SQLException e) {
                                        System.out.println("Error al consultar reporte: " + e.getMessage());
                                    }
                                    break;

                                case 3:
                                    System.out.println("======================================================================");
                                    System.out.println("- - - - - - - - 🛡️ Ver auditoría integral de inventario - - - - - - - -");
                                    System.out.println("======================================================================");
                                    try {
                                        Statement stmt = conexion.createStatement();
                                        ResultSet rs = stmt.executeQuery("SELECT '📝 NORMAL' AS tipo_evento, accion, cantidad_afectada \n" + //
                                                                                        "FROM tb_log_transacciones \n" + //
                                                                                        "WHERE accion != 'AJUSTE'\n" + //
                                                                                        "UNION ALL \n" + //
                                                                                        "SELECT '⚠️ ALERTA', 'AJUSTE/DEVOLUCION', cantidad_afectada \n" + //
                                                                                        "FROM tb_wms_alertas_ajustes \n" + //
                                                                                        "ORDER BY cantidad_afectada DESC;");
                                        
                                        System.out.println();
                                        System.out.printf("%-10s | %-12s | %-6s\n", "EVENTO", "ACCIÓN", "CANTIDAD");
                                        System.out.println("----------------------------------------------------------------------");
                                        
                                        while (rs.next()) {
                                            String evento = rs.getString("tipo_evento");
                                            String accion = rs.getString("accion");
                                            int cantidad = rs.getInt("cantidad_afectada");
                                            
                                            System.out.printf("%-8s | %-12s | %-6s\n", evento, accion, cantidad);
                                        }
                                        System.out.println("----------------------------------------------------------------------");
                                        rs.close();
                                        stmt.close();
                                    } catch (SQLException e) {
                                        System.out.println("Error al consultar el reporte: " + e.getMessage());
                                    }
                                    break;

                                case 4:
                                    System.out.println("Volviendo al Menú Principal...");

                                    break;
                            
                                default:
                                    System.out.println("Opción invalida. Intente con una opción correcta.");

                                    break;
                            }
                        } while (opcion_reportes != 4);
                        break;

                    case 5:
                        System.out.println("======================================================================");
                        System.out.println("- - - - - - - - - - - - - - ❌ Salida del Sistema - - - - - - - - - - -");
                        System.out.println("======================================================================");
                        
                        break;
                    default:
                            System.out.println("Modulo invalido. Intente con un modulo correcto.");
                        break;
                }
            } while (opcion_principal != 5);

                sc.close();
                conexion.close();
            
        } catch (SQLException e) {
            System.out.println("Error crítico al conectar con la base de datos:");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}