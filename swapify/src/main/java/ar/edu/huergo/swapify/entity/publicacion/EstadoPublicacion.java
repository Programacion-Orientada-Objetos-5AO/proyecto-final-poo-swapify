package ar.edu.huergo.swapify.entity.publicacion;

/**
 * Representa los distintos estados operativos de una publicación dentro de la
 * plataforma.
 */
public enum EstadoPublicacion {
    /** La publicación acepta nuevas ofertas. */
    ACTIVA,
    /** Existe una oferta aceptada y las partes están coordinando el intercambio. */
    EN_NEGOCIACION,
    /** El autor decidió pausar temporalmente la publicación. */
    PAUSADA,
    /** El intercambio fue concretado o la publicación dejó de estar disponible. */
    FINALIZADA;

    /**
     * Indica si el estado actual permite recibir ofertas nuevas.
     */
    public boolean admiteOfertas() {
        return this == ACTIVA;
    }

    /**
     * Señala si la publicación está actualmente visible para coordinación pero
     * no acepta nuevas ofertas.
     */
    public boolean estaReservada() {
        return this == EN_NEGOCIACION;
    }
}
