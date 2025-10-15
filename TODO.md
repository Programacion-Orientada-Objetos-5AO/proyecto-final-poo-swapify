# TODO: Agregar campo 'nombre' único como nombre de usuario visible

## Información Gathered
- Entidad Usuario actualmente usa 'username' como email, que es único y visible.
- Necesitamos agregar campo 'nombre' único, nullable inicialmente para cuentas existentes.
- DTOs: RegistrarDTO, UsuarioDTO.
- Mappers: UsuarioMapper.
- Servicios: UsuarioService para registro y validación.
- Controladores: AuthWebController (registro), CuentaWebController (mi-cuenta).
- Vistas: registro.html, seguridad.html, detalle.html, panel.html, etc.
- Publicaciones y ofertas muestran usuario.username (email), cambiar a nombre.

## Plan
1. Agregar campo 'nombre' a Usuario.java (nullable, unique).
2. Actualizar RegistrarDTO para incluir 'nombre'.
3. Actualizar UsuarioDTO para incluir 'nombre'.
4. Actualizar UsuarioMapper para mapear 'nombre'.
5. Actualizar UsuarioService para validar unicidad de 'nombre' en registro.
6. Actualizar AuthWebController para registro con 'nombre'.
7. Actualizar CuentaWebController para editar 'nombre'.
8. Actualizar vistas: registro.html agregar campo nombre, seguridad.html agregar edición de nombre.
9. Actualizar vistas de publicaciones/ofertas/admin para mostrar 'nombre' en lugar de 'username' donde corresponda.
10. Verificar y manejar cuentas existentes (nullable).

## Dependent Files to be edited
- Usuario.java
- RegistrarDTO.java
- UsuarioDTO.java
- UsuarioMapper.java
- UsuarioService.java
- AuthWebController.java
- CuentaWebController.java
- registro.html
- seguridad.html
- detalle.html
- panel.html
- Otros templates si es necesario.

## Followup steps
- Probar registro con nombre.
- Probar edición en mi-cuenta.
- Verificar vistas muestran nombre correctamente.
- Considerar migración DB si se requiere no-nullable después.
