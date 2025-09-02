package ar.edu.huergo.swapify.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.MostrarPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.ReportePublicacionesDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.service.publicacion.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final PublicacionMapper publicacionMapper;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<MostrarPublicacionDTO> crearPublicacion(@Valid @RequestBody CrearPublicacionDTO dto) {
        Publicacion publicacion = publicacionService.crearPublicacion(dto);
        return ResponseEntity.ok(publicacionMapper.toDTO(publicacion));
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportePublicacionesDTO> reportePorFecha(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<Publicacion> publicaciones = publicacionService.obtenerPublicacionesDeFecha(fecha);
        BigDecimal sumaPreciosReferenciales = publicacionService.sumaPreciosEnFecha(fecha);
        List<MostrarPublicacionDTO> publicacionesDTO = publicacionMapper.toDTOList(publicaciones);

        ReportePublicacionesDTO reporte = new ReportePublicacionesDTO(
                publicaciones.size(),
                sumaPreciosReferenciales,
                publicacionesDTO
        );
        return ResponseEntity.ok(reporte);
    }
}
