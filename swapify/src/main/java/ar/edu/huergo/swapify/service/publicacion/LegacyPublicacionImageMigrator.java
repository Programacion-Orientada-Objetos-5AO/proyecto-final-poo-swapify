package ar.edu.huergo.swapify.service.publicacion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.publicacion.PublicacionImagen;
import ar.edu.huergo.swapify.repository.publicacion.PublicacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyPublicacionImageMigrator {

    private final PublicacionRepository publicacionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateLegacyImages() {
        List<Publicacion> publicaciones = publicacionRepository.findAll();
        List<Publicacion> actualizadas = new ArrayList<>();
        for (Publicacion publicacion : publicaciones) {
            byte[] legacy = publicacion.getLegacyImagen();
            if ((legacy == null || legacy.length == 0)
                    || (publicacion.getImagenes() != null && !publicacion.getImagenes().isEmpty())) {
                continue;
            }
            PublicacionImagen imagen = new PublicacionImagen();
            imagen.setOrden(0);
            imagen.setDatos(legacy);
            String contentType = publicacion.getLegacyImagenContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/jpeg";
            }
            imagen.setContentType(contentType);
            publicacion.agregarImagen(imagen);
            publicacion.setLegacyImagen(null);
            publicacion.setLegacyImagenContentType(null);
            actualizadas.add(publicacion);
        }
        if (!actualizadas.isEmpty()) {
            publicacionRepository.saveAll(actualizadas);
            log.info("Migradas {} publicaciones con imágenes heredadas", actualizadas.size());
        }
    }
}
