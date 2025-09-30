package ar.edu.huergo.swapify.service.publicacion;

import ar.edu.huergo.swapify.dto.publicacion.CrearOfertaDTO;
import ar.edu.huergo.swapify.entity.publicacion.Oferta;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.repository.publicacion.OfertaRepository;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Oferta crearOferta(Long publicacionId, CrearOfertaDTO dto, String username) {
        if (dto == null) {
            throw new IllegalArgumentException("Datos de oferta inválidos");
        }
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Publicación no encontrada"));
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Oferta oferta = new Oferta();
        oferta.setPublicacion(publicacion);
        oferta.setUsuario(usuario);
        oferta.setMensaje(dto.getMensaje());
        oferta.setPropuestaObjeto(dto.getPropuestaObjeto());

        return ofertaRepository.save(oferta);
    }

    @Transactional
    public void eliminarPorPublicacion(Long publicacionId) {
        List<Oferta> ofertas = ofertaRepository.findByPublicacionIdOrderByFechaOfertaDesc(publicacionId);
        ofertaRepository.deleteAll(ofertas);
    }

    @Transactional
    public void eliminar(Long ofertaId) {
        ofertaRepository.deleteById(ofertaId);
    }

    @Transactional
    public List<Oferta> listarPorPublicacion(Long publicacionId) {
        return ofertaRepository.findByPublicacionIdOrderByFechaOfertaDesc(publicacionId);
    }
}
