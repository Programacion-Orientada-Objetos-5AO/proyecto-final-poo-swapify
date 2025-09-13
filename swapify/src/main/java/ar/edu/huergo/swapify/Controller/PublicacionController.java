package ar.edu.huergo.swapify.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import ar.edu.huergo.swapify.dto.publicacion.CrearPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.MostrarPublicacionDTO;
import ar.edu.huergo.swapify.dto.publicacion.ReportePublicacionesDTO;
import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import ar.edu.huergo.swapify.entity.security.Usuario;
import ar.edu.huergo.swapify.mapper.publicacion.PublicacionMapper;
import ar.edu.huergo.swapify.repository.security.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<MostrarPublicacionDTO> crearPublicacion(
            @Valid @RequestBody CrearPublicacionDTO dto,
            @AuthenticationPrincipal User principal
    ) {
        // Si no hay principal, Spring Security ya respondió 401/403 con tu EntryPoint/Handler
        String username = principal.getUsername();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Publicacion publicacion = publicacionService.crearPublicacion(dto, usuario);
        MostrarPublicacionDTO body = publicacionMapper.toDTO(publicacion);
        URI location = URI.create("/api/publicaciones/" + publicacion.getId());
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    public ResponseEntity<List<MostrarPublicacionDTO>> listarTodas() {
        List<Publicacion> entidades = publicacionService.listarTodas();
        return ResponseEntity.ok(publicacionMapper.toDTOList(entidades));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MostrarPublicacionDTO> obtenerPorId(@PathVariable Long id) {
        Publicacion p = publicacionService.obtenerPorId(id);
        return ResponseEntity.ok(publicacionMapper.toDTO(p));
    }

    @GetMapping("/reporte")
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

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<MostrarPublicacionDTO>> obtenerPublicacionesPorUsuario(@PathVariable Long usuarioId) {
        List<Publicacion> publicaciones = publicacionService.obtenerPublicacionesPorUsuario(usuarioId);
        return ResponseEntity.ok(publicacionMapper.toDTOList(publicaciones));
    }
}
