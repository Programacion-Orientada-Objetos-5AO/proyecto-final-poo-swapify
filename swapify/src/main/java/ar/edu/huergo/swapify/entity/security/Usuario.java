package ar.edu.huergo.swapify.entity.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.edu.huergo.swapify.entity.publicacion.Publicacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es requerido")
    private String username;

    @Column(nullable = true, unique = true, length = 50)
    private String nombre;

    @Column(nullable = false)
    @NotBlank(message = "La contraseña es requerida")
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Publicacion> publicaciones = new ArrayList<>();

    @Column(name = "baneado_hasta")
    private LocalDateTime baneadoHasta;

    @Column(name = "motivo_ban", length = 500)
    private String motivoBan;

    public Usuario(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean estaBaneado() {
        return baneadoHasta != null && baneadoHasta.isAfter(LocalDateTime.now());
    }

    public boolean esAdministrador() {
        return roles != null && roles.stream()
                .filter(rol -> rol != null && rol.getNombre() != null)
                .anyMatch(rol -> "ADMIN".equalsIgnoreCase(rol.getNombre()));
    }
}
