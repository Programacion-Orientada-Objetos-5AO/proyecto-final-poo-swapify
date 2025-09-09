# TODO: Modificar authorizeHttpRequests en SecurityConfig.java

## Tareas Pendientes
- [x] Actualizar authorizeHttpRequests para coincidir con los endpoints reales
  - [x] Permitir acceso público a /api/auth/login (POST)
  - [x] Permitir acceso público a /api/usuarios/registrar (POST)
  - [x] Requerir rol ADMIN para /api/usuarios (GET)
  - [x] Requerir rol CLIENTE para /api/publicaciones (POST)
  - [x] Requerir autenticación para /api/publicaciones (GET) y /api/publicaciones/{id} (GET)
  - [x] Requerir rol ADMIN para /api/publicaciones/reporte (GET)
  - [x] Requerir autenticación para cualquier otro request
- [x] Verificar que no haya endpoints faltantes en la configuración
- [ ] Probar la configuración actualizada
