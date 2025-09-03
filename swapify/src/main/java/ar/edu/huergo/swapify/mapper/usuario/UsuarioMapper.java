package ar.edu.huergo.swapify.mapper.usuario;

import ar.edu.huergo.swapify.dto.usuario.CrearUsuarioDTO;
import ar.edu.huergo.swapify.dto.usuario.MostrarUsuarioDTO;
import ar.edu.huergo.swapify.entity.usuario.Usuario;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UsuarioMapper {

    public Usuario toEntity(CrearUsuarioDTO dto) {
        if (dto == null) return null;
        Usuario u = new Usuario();
        u.setNombre(dto.getNombre());
        u.setMail(dto.getMail());
        u.setNumeroDeTelefono(dto.getNumeroDeTelefono());
        u.setContraseña(dto.getContraseña()); // cuando metas seguridad, acá va el hash
        return u;
    }

    public MostrarUsuarioDTO toDTO(Usuario u) {
        if (u == null) return null;
        return new MostrarUsuarioDTO(
                u.getId(),
                u.getNombre(),
                u.getMail(),
                u.getNumeroDeTelefono()
        );
    }

    public List<MostrarUsuarioDTO> toDTOList(List<Usuario> usuarios) {
        if (usuarios == null) return new ArrayList<>();
        return usuarios.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
