package ar.edu.huergo.swapify.mapper.publicacion;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.MostrarPublicacionDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;

/**
 * Transforma entidades de publicaciones en sus correspondientes DTO y viceversa
 * para desacoplar la capa web del modelo de datos.
 */
@Component
public class PublicacionMapper {

    /**
     * Convierte una entidad {@link Publicacion} en un DTO de visualización.
     *
     * @param publicacion entidad a transformar.
     * @return representación lista para las vistas o {@code null} si el origen es
     *         {@code null}.
     */
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

    /**
     * Convierte una lista de publicaciones en sus DTO correspondientes.
     *
     * @param publicaciones coleccion de entidades a mapear.
     * @return lista nunca nula de DTO.
     */
    public List<MostrarPublicacionDTO> toDTOList(List<Publicacion> publicaciones) {
        if (publicaciones == null) {
            return new ArrayList<>();
        }
        return publicaciones.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Construye una entidad base a partir del DTO de creación omitiendo los
     * atributos calculados por la persistencia.
     *
     * @param dto datos recibidos desde el formulario de creación.
     * @return nueva instancia inicializada con los valores del DTO.
     */
    public Publicacion toEntity(CrearPublicacionDTO dto) {
        if (dto == null) {
            return null;
        }
        Publicacion p = new Publicacion();
        p.setNombre(dto.getNombre());
        p.setPrecio(dto.getPrecio());
        p.setDescripcion(dto.getDescripcion());
        p.setObjetoACambiar(dto.getObjetoACambiar());
        return p;
    }
}
