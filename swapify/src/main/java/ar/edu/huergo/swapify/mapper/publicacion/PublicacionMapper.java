package ar.edu.huergo.swapify.mapper.publicacion;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.MostrarPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;

@Component
public class PublicacionMapper {

    public MostrarPublicacionDTO toDTO(Publicacion publicacion) {
        if (publicacion == null) {
            return null;
        }
        return new MostrarPublicacionDTO(
                publicacion.getId(),
                publicacion.getNombre(),
                publicacion.getPrecio(),
                publicacion.getDescripcion(),
                publicacion.getObjetoACambiar(),
                publicacion.getFechaPublicacion(),
                publicacion.getUsuario() != null ? publicacion.getUsuario().getUsername() : null,
                publicacion.tieneImagen() ? Base64.getEncoder().encodeToString(publicacion.getImagen()) : null,
                publicacion.getImagenContentType()
        );
    }

    public List<MostrarPublicacionDTO> toDTOList(List<Publicacion> publicaciones) {
        if (publicaciones == null) {
            return new ArrayList<>();
        }
        return publicaciones.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Opcional: para crear la entidad desde el DTO de creación
    public Publicacion toEntity(CrearPublicacionDTO dto) {
        if (dto == null) {
            return null;
        }
        Publicacion p = new Publicacion();
        p.setNombre(dto.getNombre());
        p.setPrecio(dto.getPrecio());
        p.setDescripcion(dto.getDescripcion());
        p.setObjetoACambiar(dto.getObjetoACambiar());
        // fechaPublicacion se setea en @PrePersist de la entidad
        return p;
    }
}
