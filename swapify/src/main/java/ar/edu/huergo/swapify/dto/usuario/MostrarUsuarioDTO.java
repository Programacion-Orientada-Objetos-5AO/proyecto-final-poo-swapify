package ar.edu.huergo.swapify.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MostrarUsuarioDTO {
    private Long id;
    private String nombre;
    private String mail;
    private String numeroDeTelefono;
}
