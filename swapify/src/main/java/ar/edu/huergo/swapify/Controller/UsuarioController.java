package ar.edu.huergo.swapify.Controller;

import ar.edu.huergo.swapify.dto.usuario.CrearUsuarioDTO;
import ar.edu.huergo.swapify.dto.usuario.MostrarUsuarioDTO;
import ar.edu.huergo.swapify.entity.usuario.Usuario;
import ar.edu.huergo.swapify.mapper.usuario.UsuarioMapper;
import ar.edu.huergo.swapify.service.usuario.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    // POST /api/usuarios  -> crea usuario
    @PostMapping
    public ResponseEntity<MostrarUsuarioDTO> crear(@Valid @RequestBody CrearUsuarioDTO dto) {
        try {
            Usuario u = usuarioService.crear(dto);
            return ResponseEntity.ok(usuarioMapper.toDTO(u));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/usuarios  -> lista todos
    @GetMapping
    public ResponseEntity<List<MostrarUsuarioDTO>> listar() {
        try {
            List<Usuario> list = usuarioService.listarTodos();
            return ResponseEntity.ok(usuarioMapper.toDTOList(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/usuarios/{id} -> trae uno
    @GetMapping("/{id}")
    public ResponseEntity<MostrarUsuarioDTO> obtener(@PathVariable Long id) {
        try {
            Usuario u = usuarioService.obtenerPorId(id);
            return ResponseEntity.ok(usuarioMapper.toDTO(u));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
