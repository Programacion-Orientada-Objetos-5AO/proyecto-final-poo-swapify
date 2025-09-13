package ar.edu.huergo.swapify.dto.security;

import java.util.List;

public record UsuarioDTO(String username, List<String> roles) {
    
}
