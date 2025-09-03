package ar.edu.huergo.swapify.service.usuario;

import ar.edu.huergo.swapify.dto.usuario.CrearUsuarioDTO;
import ar.edu.huergo.swapify.entity.usuario.Usuario;
import ar.edu.huergo.swapify.mapper.usuario.UsuarioMapper;
import ar.edu.huergo.swapify.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public Usuario crear(CrearUsuarioDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Datos de usuario inválidos");
        if (usuarioRepository.existsByMailIgnoreCase(dto.getMail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese mail");
        }
        Usuario u = usuarioMapper.toEntity(dto);
        // Más adelante: u.setContraseña(encoder.encode(dto.getContraseña()));
        return usuarioRepository.save(u);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }
}
