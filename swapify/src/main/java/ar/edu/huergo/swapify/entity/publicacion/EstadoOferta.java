package ar.edu.huergo.swapify.entity.publicacion;

public enum EstadoOferta {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA;

    public boolean esFinalizada() {
        return this == ACEPTADA || this == RECHAZADA;
    }
}
