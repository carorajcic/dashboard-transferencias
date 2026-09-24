# Dashboard de Transferencias — Primera Entrega

## Contenido de esta entrega
1. **Modelo canónico de la base de datos** → `src/main/resources/db/01_modelo_canonico.sql`
2. **Datos iniciales de carga** → `src/main/resources/db/02_datos_iniciales.sql`
3. **Sistema de login funcionando** → módulo completo Spring Boot + Spring Security

## Cómo levantar el proyecto

### 1. Crear la base de datos
```sql
CREATE DATABASE dashboard_transferencias;
```

### 2. Ejecutar los scripts SQL en orden
```
psql -U postgres -d dashboard_transferencias -f src/main/resources/db/01_modelo_canonico.sql
psql -U postgres -d dashboard_transferencias -f src/main/resources/db/02_datos_iniciales.sql
```

### 3. Ajustar credenciales de conexión
Editar `src/main/resources/application.properties` con tu usuario/contraseña de PostgreSQL local.

### 4. Levantar la aplicación
```
mvn spring-boot:run
```

### 5. Probar el login
Ir a `http://localhost:8080/login`

- **Usuario:** `admin`
- **Contraseña:** `admin123`

Si las credenciales son correctas, redirige automáticamente a `/tablero`.
Si son incorrectas, vuelve a `/login` con un mensaje de error y no permite el acceso.

## Nota sobre el hash de la contraseña
El `password_hash` cargado en `02_datos_iniciales.sql` corresponde a la
contraseña `admin123` encriptada con BCrypt. Si querés generar tu propio
usuario con otra contraseña, podés hacerlo con este código de una sola vez:

```java
System.out.println(new BCryptPasswordEncoder().encode("tu_password"));
```

Y pegar el resultado en un INSERT nuevo sobre la tabla `usuarios`.
