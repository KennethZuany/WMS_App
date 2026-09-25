# Zuany WMS

Sistema de gestión de almacenes (WMS) para administrar productos, ubicaciones,
movimientos de inventario y reportes operativos.

La aplicación ofrece dos modos de uso:

1. **Consola** — `App.java`, la interfaz original basada en terminal.
2. **Web UI** — `ApiServer.java` + `web/`, interfaz gráfica moderna en el
   navegador.

Ambos se conectan a PostgreSQL a través de Supabase usando JDBC.

## Qué resuelve

| Módulo | Capacidades |
| --- | --- |
| Productos | Consultar catálogo, registrar productos, editar descripciones y dar de baja SKUs. |
| Ubicaciones | Consultar, registrar, modificar y eliminar ubicaciones del almacén. |
| Movimientos | Registrar recepciones de stock, realizar conteos cíclicos y consultar el historial. |
| Reportes | Consultar stock total, ocupación del almacén y auditoría integral del inventario. |

## Stack

- **Java** para la aplicación de consola y el servidor API.
- **JDBC** para la comunicación con PostgreSQL.
- **Supabase** como proveedor de base de datos.
- **PostgreSQL JDBC Driver 42.7.13**, incluido en `lib/`.
- **HTML / CSS / JavaScript** para la interfaz web.
- **Visual Studio Code** con Extension Pack for Java (opcional, pero recomendado).

## Requisitos

- Java JDK 17 o superior.
- Una base PostgreSQL accesible desde Internet, o un proyecto de Supabase.
- El esquema de base de datos y sus funciones almacenadas desplegados antes de
	iniciar la aplicación.

La aplicación espera, entre otras, las siguientes tablas y funciones:

- `tb_sys_productos`
- `tb_sys_categorias`
- `tb_wms_ubicaciones`
- `tb_wms_inventario`
- `tb_log_transacciones`
- `tb_usr_empleados`
- `sp_registras_ubicacion`, `sp_reporte_stock` y las funciones usadas por los
	reportes de auditoría y ocupación.

## Configuración de la conexión

Antes de ejecutar el proyecto, configura en `src/App.java` (consola) o
`src/ApiServer.java` (web) la URL, el usuario y la contraseña de tu instancia
de PostgreSQL/Supabase.

> **Importante:** actualmente las credenciales están escritas directamente en
> el código. No subas contraseñas reales al repositorio. Para un entorno serio,
> muévelas a variables de entorno o a un gestor de secretos y rota cualquier
> credencial que haya sido expuesta.

La conexión actual usa el pooler de Supabase y SSL:

```text
jdbc:postgresql://<host>:6543/postgres?sslmode=require
```

## Ejecutar la interfaz de consola

Desde la raíz del proyecto:

```bash
# macOS/Linux
rm -rf bin
mkdir -p bin
javac -cp "lib/postgresql-42.7.13.jar" -d bin src/App.java
java -cp "bin:lib/postgresql-42.7.13.jar" App
```

En Windows, usa `;` en lugar de `:` al separar rutas del classpath:

```powershell
javac -cp "lib\postgresql-42.7.13.jar" -d bin src\App.java
java -cp "bin;lib\postgresql-42.7.13.jar" App
```

## Ejecutar la interfaz web

```bash
# macOS/Linux
rm -rf bin
mkdir -p bin
javac -cp "lib/postgresql-42.7.13.jar" -d bin src/ApiServer.java
java -cp "bin:lib/postgresql-42.7.13.jar" ApiServer
```

Después abre tu navegador en **http://localhost:8080**.

El servidor levanta un API REST en Java y sirve los archivos estáticos de
`web/` automáticamente.

## Ejecutar desde VS Code

1. Abre la carpeta del proyecto en VS Code.
2. Instala el **Extension Pack for Java** si VS Code lo solicita.
3. Verifica que el JDK seleccionado sea 17 o superior.
4. Abre `src/App.java` (consola) o `src/ApiServer.java` (web) y pulsa
   **Run Java**.

La configuración de `.vscode/settings.json` ya reconoce `src/` como origen,
`bin/` como salida y todos los JAR dentro de `lib/` como dependencias.

## Flujo de uso

### Consola

Al iniciar, el sistema intenta conectarse a la base de datos y muestra el menú
principal. Selecciona una opción escribiendo el número correspondiente:

```text
1. Módulo de Productos
2. Módulo de Ubicaciones
3. Módulo de Movimientos
4. Módulo de Reportes Generales
5. Salir del Sistema
```

### Web

Al iniciar, el navegador muestra la pantalla de inicio con accesos directos a
cada módulo. Navega entre secciones usando la barra superior. Cada módulo tiene
pestañas internas para consultar, crear, editar y eliminar registros.

Los movimientos requieren que existan previamente el usuario, el producto y la
ubicación relacionados en la base de datos.

## Estructura del proyecto

```text
WMS_App/
├── src/
│   ├── App.java                    # Menú y lógica JDBC (consola)
│   └── ApiServer.java              # Servidor HTTP + API REST (web)
├── web/
│   ├── index.html                  # Interfaz web principal
│   ├── style.css                   # Estilos (dark theme, glassmorphism)
│   └── app.js                      # Lógica del front-end
├── lib/
│   └── postgresql-42.7.13.jar     # Driver JDBC de PostgreSQL
├── bin/                            # .class generados al compilar
├── .vscode/
│   └── settings.json               # Configuración del proyecto Java
└── README.md
```

## Solución de problemas

**`ClassNotFoundException: org.postgresql.Driver`**

Comprueba que `lib/postgresql-42.7.13.jar` exista y que el classpath incluya el
JAR tanto al compilar como al ejecutar.

**Error de conexión o timeout**

Valida las credenciales, el host, el puerto `6543`, el acceso a Internet y que
`sslmode=require` esté presente en la URL.

**`relation does not exist` o `function does not exist`**

El esquema requerido aún no está desplegado en la base de datos, o los nombres
de tablas y funciones no coinciden con los que utiliza `App.java`.

## Próximas mejoras

- Leer credenciales desde variables de entorno.
- Separar la conexión, los repositorios y la interfaz de consola en clases
	independientes.
- Agregar validación de entradas y pruebas automatizadas.
- Incorporar scripts versionados para crear y poblar el esquema de la base de
	datos.
