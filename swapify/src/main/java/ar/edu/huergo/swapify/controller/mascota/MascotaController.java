package ar.edu.huergo.swapify.controller.mascota;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import ar.edu.huergo.swapify.dto.mascota.MascotaRequestDTO;
import ar.edu.huergo.swapify.dto.mascota.MascotaResponseDTO;
import ar.edu.huergo.swapify.service.mascota.MascotaService;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {
    
    @Autowired
    private MascotaService mascotaService;

    @PostMapping
    public ResponseEntity<?> crearMascota(@RequestBody MascotaRequestDTO mascotaRequestDTO) {
            MascotaResponseDTO responseDTO = mascotaService.crearMascota(mascotaRequestDTO);
            return ResponseEntity.ok(responseDTO);
    }

    @GetMapping
    public ResponseEntity<List<MascotaResponseDTO>> recibirMascotas() {
        List<MascotaResponseDTO> mascotas = mascotaService.recibirMascotas();
        return ResponseEntity.ok(mascotas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> recibirMascotaId(@PathVariable Long id) {
            MascotaResponseDTO responseDTO = mascotaService.recibirMascotaId(id);
            return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMascota (@PathVariable Long id, @RequestBody MascotaRequestDTO mascotaRequestDTO) {
        MascotaResponseDTO responseDTO = mascotaService.actualizarMascota(id, mascotaRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> borrarMascota (@PathVariable Long id) {
            mascotaService.borrarMascota(id);
            return ResponseEntity.noContent().build();
    }    
}
