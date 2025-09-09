package ar.edu.huergo.swapify.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.MostrarPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.ReportePublicacionesDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final PublicacionMapper publicacionMapper;

    // POST /api/publicaciones  -> crea una publicación
    @PostMapping
    public ResponseEntity<MostrarPublicacionDTO> crearPublicacion(@Valid @RequestBody CrearPublicacionDTO dto) {
        try {
            Publicacion publicacion = publicacionService.crearPublicacion(dto);
            return ResponseEntity.ok(publicacionMapper.toDTO(publicacion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/publicaciones  -> lista todas las publicaciones
    @GetMapping
    public ResponseEntity<List<MostrarPublicacionDTO>> listarTodas() {
        try {
            List<Publicacion> entidades = publicacionService.listarTodas();
            return ResponseEntity.ok(publicacionMapper.toDTOList(entidades));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/publicaciones/{id}  -> obtiene una publicación por id
    @GetMapping("/{id}")
    public ResponseEntity<MostrarPublicacionDTO> obtenerPorId(@PathVariable Long id) {
        try {
            Publicacion p = publicacionService.obtenerPorId(id);
            return ResponseEntity.ok(publicacionMapper.toDTO(p));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/publicaciones/reporte?fecha=YYYY-MM-DD  -> reporte por fecha
    @GetMapping("/reporte")
    public ResponseEntity<ReportePublicacionesDTO> reportePorFecha(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            List<Publicacion> publicaciones = publicacionService.obtenerPublicacionesDeFecha(fecha);
            BigDecimal sumaPreciosReferenciales = publicacionService.sumaPreciosEnFecha(fecha);
            List<MostrarPublicacionDTO> publicacionesDTO = publicacionMapper.toDTOList(publicaciones);

            ReportePublicacionesDTO reporte = new ReportePublicacionesDTO(
                    publicaciones.size(),
                    sumaPreciosReferenciales,
                    publicacionesDTO
            );
            return ResponseEntity.ok(reporte);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
