package ar.edu.huergo.swapify.config.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicacionSchemaMigration implements ApplicationRunner {

    private static final String TABLA_PUBLICACION = "PUBLICACION";
    private static final String COL_ESTADO = "ESTADO";
    private static final String COL_ES_OFICIAL = "ES_OFICIAL";
    private static final String COL_FECHA_RESERVA = "FECHA_RESERVA";
    private static final String COL_FECHA_CIERRE = "FECHA_CIERRE";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tablaPublicacionExiste()) {
                log.debug("La tabla PUBLICACION aún no existe. Hibernate se encargará de crearla con el nuevo esquema.");
                return;
            }
            asegurarColumnaEstado();
            asegurarColumnaEsOficial();
            asegurarColumnaFechaReserva();
            asegurarColumnaFechaCierre();
        } catch (Exception e) {
            log.error("No se pudo preparar el esquema de publicaciones", e);
        }
    }

    private boolean tablaPublicacionExiste() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?",
                Integer.class,
                TABLA_PUBLICACION);
        return count != null && count > 0;
    }

    private void asegurarColumnaEstado() {
        if (columnaExiste(COL_ESTADO)) {
            return;
        }
        log.info("Agregando columna ESTADO a la tabla PUBLICACION");
        jdbcTemplate.execute("ALTER TABLE PUBLICACION ADD COLUMN ESTADO VARCHAR(20)");
        jdbcTemplate.update("UPDATE PUBLICACION SET ESTADO = 'ACTIVA' WHERE ESTADO IS NULL");
    }

    private void asegurarColumnaEsOficial() {
        if (columnaExiste(COL_ES_OFICIAL)) {
            return;
        }
        log.info("Agregando columna ES_OFICIAL a la tabla PUBLICACION");
        jdbcTemplate.execute("ALTER TABLE PUBLICACION ADD COLUMN ES_OFICIAL BOOLEAN DEFAULT FALSE");
        jdbcTemplate.update("UPDATE PUBLICACION SET ES_OFICIAL = FALSE WHERE ES_OFICIAL IS NULL");
    }

    private void asegurarColumnaFechaReserva() {
        if (columnaExiste(COL_FECHA_RESERVA)) {
            return;
        }
        log.info("Agregando columna FECHA_RESERVA a la tabla PUBLICACION");
        jdbcTemplate.execute("ALTER TABLE PUBLICACION ADD COLUMN FECHA_RESERVA TIMESTAMP(6)");
    }

    private void asegurarColumnaFechaCierre() {
        if (columnaExiste(COL_FECHA_CIERRE)) {
            return;
        }
        log.info("Agregando columna FECHA_CIERRE a la tabla PUBLICACION");
        jdbcTemplate.execute("ALTER TABLE PUBLICACION ADD COLUMN FECHA_CIERRE TIMESTAMP(6)");
    }

    private boolean columnaExiste(String nombreColumna) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?",
                    Integer.class,
                    TABLA_PUBLICACION,
                    nombreColumna);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.warn("No se pudo comprobar la existencia de la columna {}", nombreColumna, e);
            return false;
        }
    }
}
