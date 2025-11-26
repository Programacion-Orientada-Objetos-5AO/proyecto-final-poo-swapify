package ar.edu.huergo.swapify.entity.prestamo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Prestamo")
@Data
@NoArgsConstructor
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo_libro", nullable = false, length = 200)
    private String tituloLibro;

    @Column(name = "nombre_usuario", nullable = false, length = 120)
    private String nombreUsuario;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDate fechaPrestamo;

    @Column(name = "fecha_devolucion", nullable = false)
    private LocalDate fechaDevolucion;

    @Column(name = "devuelto", nullable = false)
    private boolean devuelto;

    @Column(name = "dias_prestamo", nullable = false)
    private Integer diasPrestamo;

    @Column(name = "fecha_devolucion_real")
    private LocalDate fechaDevolucionReal;

    @PrePersist
    public void prePersist() {
        if (fechaPrestamo == null) {
            fechaPrestamo = LocalDate.now();
        }
        if (diasPrestamo != null && diasPrestamo > 0 && fechaDevolucion == null) {
            fechaDevolucion = fechaPrestamo.plusDays(diasPrestamo);
        }
    }
}
