package com.Monarca.Backend.controller;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class ApiErroresTest {
    @Test
    void correlacionaRestriccionSinExponerValoresSQL(CapturedOutput output) {
        var sql = new SQLException("correo=privado@example.test", "23514");
        var causa = new ConstraintViolationException("datos privados", sql, "pagos_estado_check");
        var respuesta = new ApiErrores().conflicto(new DataIntegrityViolationException("datos privados", causa));
        assertEquals(409, respuesta.getStatusCode().value());
        var cuerpo = (Map<?, ?>) respuesta.getBody();
        String incidente = (String) cuerpo.get("incidente");
        assertNotNull(UUID.fromString(incidente));
        assertTrue(output.getOut().contains(incidente));
        assertTrue(output.getOut().contains("SQLSTATE=23514"));
        assertTrue(output.getOut().contains("restriccion=pagos_estado_check"));
        assertFalse(output.getAll().contains("privado@example.test"));
        assertFalse(cuerpo.toString().contains("pagos_estado_check"));
    }

    @Test
    void conservaConflictoSinCausaSQL() {
        assertEquals(409, new ApiErrores().conflicto(new DataIntegrityViolationException("detalle interno")).getStatusCode().value());
    }
}
