# Plan para arreglar el login web para usar JWT automáticamente

## Información Recopilada
- La aplicación tiene partes web (Thymeleaf) y API (JSON).
- Actualmente, el login web usa sesiones de Spring Security.
- El login API usa JWT.
- El formulario de login envía POST a /login, pero en lugar de redirigir a la página principal, redirige a /login y muestra el token.
- Para usar el token automáticamente, necesitamos cambiar el login web para obtener el JWT y usarlo en las solicitudes subsiguientes.

## Plan
1. **Modificar SecurityConfig.java**: Deshabilitar formLogin y logout automático, ya que manejaremos login manualmente.
2. **Modificar JwtAuthenticationFilter.java**: Agregar verificación de token en cookies además de headers.
3. **Modificar AuthWebController.java**: Agregar endpoint POST /web/login que llame al servicio de autenticación y devuelva el token.
4. **Modificar login.html**: Cambiar el formulario para usar AJAX para llamar a /web/login, obtener el token, almacenarlo en localStorage y cookie, y redirigir.
5. **Modificar app.js**: Agregar lógica para incluir el token en solicitudes AJAX (si las hay), y modificar logout para limpiar cookie y localStorage.
6. **Modificar base.html**: Agregar script para incluir token en headers si es necesario para futuras solicitudes AJAX.

## Archivos Dependientes
- SecurityConfig.java
- JwtAuthenticationFilter.java
- AuthWebController.java
- login.html
- app.js
- base.html

## Pasos de Seguimiento
- Probar el login después de los cambios.
- Verificar que el token se almacene correctamente.
- Verificar que las páginas web funcionen con JWT.
- Probar logout.
