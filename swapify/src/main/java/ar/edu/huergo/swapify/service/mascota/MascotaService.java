package ar.edu.huergo.swapify.service.mascota;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import ar.edu.huergo.swapify.dto.mascota.MascotaRequestDTO;
import ar.edu.huergo.swapify.dto.mascota.MascotaResponseDTO;
import ar.edu.huergo.swapify.entity.mascota.Mascota;
import ar.edu.huergo.swapify.mapper.mascota.MascotaMapper;
import ar.edu.huergo.swapify.repository.mascota.MascotaRepository;

@Service
@Transactional
public class MascotaService {
    
    
    private final MascotaRepository mascotaRepository;
    private final MascotaMapper mascotaMapper;

    public MascotaService(MascotaRepository mascotaRepository, MascotaMapper mascotaMapper) {
        this.mascotaRepository = mascotaRepository;
        this.mascotaMapper = mascotaMapper;
    }

    public MascotaResponseDTO crearMascota(MascotaRequestDTO requestDTO) {
        Mascota mascota = mascotaMapper.toEntity(requestDTO);
        mascota = mascotaRepository.save(mascota);
        return mascotaMapper.toDTO(mascota);
    }

    public List<MascotaResponseDTO> recibirMascotas() {
        List<Mascota> mascotas = mascotaRepository.findAll();
        return mascotas.stream().map(mascotaMapper::toDTO).collect(Collectors.toList());
    }

    public MascotaResponseDTO recibirMascotaId(Long id) {
        Mascota mascota = mascotaRepository.findById(id).orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
        return mascotaMapper.toDTO(mascota);
    }

    public MascotaResponseDTO actualizarMascota(Long id, MascotaRequestDTO requestDTO) {
        Mascota mascota = mascotaRepository.findById(id).orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
        mascota.setNombre(requestDTO.getNombre());
        mascota.setTipo(requestDTO.getTipo());
        mascota.setEdad(requestDTO.getEdad());
        mascota = mascotaRepository.save(mascota);
        return mascotaMapper.toDTO(mascota);
    }

    public void borrarMascota(Long id) {
        mascotaRepository.deleteById(id);
    }

}
