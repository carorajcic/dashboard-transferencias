# Dashboard de Transferencias

Proyecto final de Tableros y Dashboards (UAI). Tablero de control sobre transferencias de
fondeo de cuenta (AFT) entre bancos, con drill-down de 3 niveles, semáforo y login.

## Qué incluye

| Requisito de la consigna | Cómo se resuelve |
|---|---|
| Consultas SQL | Todos los datos salen de `SELECT` en `TableroRepository.java` |
| Drill-down / drill-up (3 niveles) | Bancos → Transferencias del banco → Detalle de la transferencia |
| Semaforización (3+ estados) | Tasa de aprobación por banco, calculada en SQL (verde / amarillo / rojo / sin datos) |
| Login contra tabla de usuarios | Spring Security + tabla `usuarios` con contraseña hasheada con BCrypt |
| 4 entidades | Bancos, Productos, Países y Transferencias (más la tabla puente `bancos_productos`) |
| Sin ABM | Solo lectura; los datos se cargan con `INSERT` |

## Tecnologías

Java 17, Spring Boot 3.3.4, Spring Security, Spring Data JPA / JdbcTemplate, Thymeleaf,
PostgreSQL, Chart.js y Maven.

## Puesta en marcha

### 1. Base de datos

Crear la base y ejecutar los scripts **en orden**:

```
psql -U postgres -c "CREATE DATABASE dashboard_transferencias;"
psql -U postgres -d dashboard_transferencias -f src/main/resources/db/01_modelo_canonico.sql
psql -U postgres -d dashboard_transferencias -f src/main/resources/db/02_datos_iniciales.sql
psql -U postgres -d dashboard_transferencias -f src/main/resources/db/03_ampliacion_datos.sql
```

- `01` crea las tablas, `02` carga datos básicos y el usuario de prueba.
- `03` crea la tabla `codigos_respuesta` y genera unas 600 transferencias de los últimos
  30 días. **Ejecutarlo una sola vez**, porque volver a correrlo duplica las transferencias.

### 2. Conexión

Revisar usuario y contraseña de PostgreSQL en `src/main/resources/application.properties`
(por defecto `postgres` / `postgres`, puerto 5432).

### 3. Levantar la aplicación

Desde el IDE (clase `DashboardTransferenciasApplication`) o por consola:

```
mvn spring-boot:run
```

### 4. Ingresar

Abrir `http://localhost:8080/login` (escribir la dirección en el navegador; no abrir el
archivo HTML directamente).

- **Usuario:** `admin`
- **Contraseña:** `admin123`

> Los gráficos usan Chart.js desde un CDN, por lo que hace falta conexión a internet.

## Pantallas

| Nivel | URL | Contenido |
|---|---|---|
| Login | `/login` | Validación contra la tabla `usuarios` |
| 1. Bancos | `/tablero` | Indicadores generales, gráfico de estados, tabla de bancos con semáforo, filtro por fechas |
| 2. Transferencias del banco | `/tablero/banco/{id}` | Indicadores del banco, evolución diaria, listado filtrable por estado y país |
| 3. Detalle | `/tablero/transferencia/{id}` | Datos completos, código de respuesta y línea de tiempo |

Cada nivel tiene un enlace para volver al anterior (drill-up) y conserva el filtro de fechas.

## Semáforo

Tasa de aprobación = operaciones aprobadas / operaciones finalizadas (se excluyen las pendientes).

| Estado | Condición | Significado |
|---|---|---|
| Verde | tasa >= 90% | Operación normal |
| Amarillo | 75% a menos de 90% | Advertencia |
| Rojo | menos de 75% | Situación crítica |
| Gris | sin operaciones finalizadas | Sin datos |

Los umbrales se configuran en `application.properties`
(`tablero.semaforo.verde` y `tablero.semaforo.amarillo`).

## Estructura del proyecto

```
src/main/java/com/uai/dashboard/
  config/        SecurityConfig (login, BCrypt, rutas protegidas)
  controller/    LoginController, TableroController (3 niveles)
  dto/           Resultados de las consultas
  entity/        Usuario
  repository/    UsuarioRepository, TableroRepository (consultas SQL)
  service/       UsuarioDetailsService (autenticación contra la base)
src/main/resources/
  db/            Scripts SQL (modelo, datos iniciales, ampliación)
  templates/     login, tablero-nivel1/2/3, fragments
  static/        css y js
wireframes-ui.html   Bocetos de las pantallas
informe/             Informe del proyecto
```

## Usuarios y contraseñas

La contraseña se guarda hasheada con BCrypt en `usuarios.password_hash`; nunca en texto plano.
Para crear otro usuario, generar el hash con:

```java
System.out.println(new BCryptPasswordEncoder().encode("tu_password"));
```

y cargarlo con un `INSERT` en la tabla `usuarios`.
