package ar.edu.huergo.swapify.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.huergo.swapify.dto.prestamo.PrestamoRequestDTO;
import ar.edu.huergo.swapify.dto.prestamo.PrestamoResponseDTO;
import ar.edu.huergo.swapify.dto.prestamo.ResumenUsuarioDTO;
import ar.edu.huergo.swapify.service.prestamo.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoResponseDTO> crear(@RequestBody @Valid PrestamoRequestDTO request) {
        PrestamoResponseDTO prestamo = prestamoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamo);
    }

    @GetMapping
    public List<PrestamoResponseDTO> listar() {
        return prestamoService.listar();
    }

    @GetMapping("/{id}")
    public PrestamoResponseDTO obtener(@PathVariable Long id) {
        return prestamoService.obtener(id);
    }

    @PutMapping("/{id}")
    public PrestamoResponseDTO actualizar(@PathVariable Long id, @RequestBody @Valid PrestamoRequestDTO request) {
        return prestamoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/devolver")
    public PrestamoResponseDTO marcarDevuelto(@PathVariable Long id) {
        return prestamoService.marcarDevuelto(id);
    }

    @GetMapping("/vencidos")
    public List<PrestamoResponseDTO> vencidos() {
        return prestamoService.listarVencidos();
    }

    @GetMapping("/historial")
    public List<PrestamoResponseDTO> historial(@RequestParam("usuario") String usuario) {
        return prestamoService.listarPorUsuario(usuario);
    }

    @GetMapping("/resumen")
    public ResumenUsuarioDTO resumen(@RequestParam("usuario") String usuario) {
        return prestamoService.resumenPorUsuario(usuario);
    }
}
