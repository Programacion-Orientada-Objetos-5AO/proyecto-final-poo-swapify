package ar.edu.huergo.swapify.config.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajusta la tabla OFERTA para garantizar que existan las columnas necesarias
 * para el flujo de aceptación/rechazo y normaliza los valores legacy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfertaSchemaMigration implements ApplicationRunner {

    private static final String TABLA_OFERTA = "OFERTA";
    private static final String COL_ESTADO = "ESTADO";
    private static final String COL_FECHA_RESPUESTA = "FECHA_RESPUESTA";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tablaOfertaExiste()) {
                log.debug("La tabla OFERTA aún no existe. Hibernate la creará con el nuevo esquema.");
                return;
            }
            asegurarColumnaEstado();
            asegurarColumnaFechaRespuesta();
            normalizarEstadosInconsistentes();
        } catch (Exception e) {
            log.error("No se pudo preparar el esquema de ofertas", e);
        }
    }

    private boolean tablaOfertaExiste() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?",
                Integer.class,
                TABLA_OFERTA);
        return count != null && count > 0;
    }

    private void asegurarColumnaEstado() {
        if (columnaExiste(COL_ESTADO)) {
            return;
        }
        log.info("Agregando columna ESTADO a la tabla OFERTA");
        jdbcTemplate.execute("ALTER TABLE OFERTA ADD COLUMN ESTADO VARCHAR(20)");
        jdbcTemplate.update("UPDATE OFERTA SET ESTADO = 'PENDIENTE' WHERE ESTADO IS NULL");
    }

    private void asegurarColumnaFechaRespuesta() {
        if (columnaExiste(COL_FECHA_RESPUESTA)) {
            return;
        }
        log.info("Agregando columna FECHA_RESPUESTA a la tabla OFERTA");
        jdbcTemplate.execute("ALTER TABLE OFERTA ADD COLUMN FECHA_RESPUESTA TIMESTAMP(6)");
    }

    private void normalizarEstadosInconsistentes() {
        try {
            jdbcTemplate.update("UPDATE OFERTA SET ESTADO = 'PENDIENTE' WHERE ESTADO IS NULL OR TRIM(ESTADO) = ''");
            jdbcTemplate.update("""
                    UPDATE OFERTA
                    SET ESTADO = 'PENDIENTE'
                    WHERE ESTADO IS NOT NULL
                      AND UPPER(ESTADO) NOT IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA')
                    """);
            jdbcTemplate.update("UPDATE OFERTA SET ESTADO = UPPER(ESTADO) WHERE ESTADO IS NOT NULL");
        } catch (DataAccessException e) {
            log.warn("No se pudieron normalizar los estados legacy de ofertas", e);
        }
    }

    private boolean columnaExiste(String nombreColumna) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?",
                Integer.class,
                TABLA_OFERTA,
                nombreColumna);
        return count != null && count > 0;
    }
}
