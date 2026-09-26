package com.Monarca.Backend.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
import java.util.UUID;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class ApiErrores {
    private static final Logger log = LoggerFactory.getLogger(ApiErrores.class);
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> imagenGrande() { return ResponseEntity.status(413).body(Map.of("error", "La foto supera los 5 MB permitidos.")); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> validacion(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> estado(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "Solicitud no disponible" : e.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflicto(DataIntegrityViolationException e) {
        String incidente = UUID.randomUUID().toString();
        String sqlState = "desconocido", restriccion = "desconocida";
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) sqlState = sql.getSQLState();
            if (causa instanceof org.hibernate.exception.ConstraintViolationException constraint)
                restriccion = constraint.getConstraintName();
        }
        // No registrar mensajes SQL ni valores de filas: pueden contener datos personales.
        log.warn("Conflicto de integridad: incidente={} SQLSTATE={} restriccion={}", incidente, sqlState, restriccion);
        return ResponseEntity.status(409).body(Map.of("error", "Los datos ya existen o no cumplen las restricciones. Revisa los campos. Si persiste, comunica este identificador: " + incidente, "incidente", incidente));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> formato() { return ResponseEntity.badRequest().body(Map.of("error", "Revisa el formato y los tipos de los campos enviados.")); }
}
