package ar.edu.huergo.swapify.mapper.mascota;

import org.springframework.stereotype.Component;
import ar.edu.huergo.swapify.dto.mascota.MascotaRequestDTO;
import ar.edu.huergo.swapify.dto.mascota.MascotaResponseDTO;
import ar.edu.huergo.swapify.entity.mascota.Mascota;

@Component
public class MascotaMapper {

    public Mascota toEntity(MascotaRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Mascota mascota = new Mascota();
        mascota.setNombre(dto.getNombre());
        mascota.setTipo(dto.getTipo());
        mascota.setEdad(dto.getEdad());
        return mascota;
    }

    public MascotaResponseDTO toDTO(Mascota mascota) {
        if (mascota == null) {
            return null;
        }
        MascotaResponseDTO dto = new MascotaResponseDTO();
        dto.setId(mascota.getId());
        dto.setNombre(mascota.getNombre());
        dto.setTipo(mascota.getTipo());
        dto.setEdad(mascota.getEdad());
        dto.setAdoptado(mascota.getAdoptado());
        return dto;
    }

}