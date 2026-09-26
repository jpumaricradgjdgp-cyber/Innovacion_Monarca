package com.Monarca.Backend.security;
import java.time.Instant;
import java.util.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
public class LimiteAcceso {
    private record Ventana(int cantidad, Instant vence) {}
    private final Map<String,Ventana> intentos = new HashMap<>();
    public synchronized void comprobar(String clave, int maximo) {
        verificar(clave, maximo);
        registrarFallo(clave);
    }
    public synchronized void verificar(String clave, int maximo) {
        Instant ahora = Instant.now();
        intentos.entrySet().removeIf(e -> !e.getValue().vence().isAfter(ahora));
        Ventana v = intentos.get(clave);
        if ((v != null && v.cantidad() >= maximo) || (v == null && intentos.size() >= 10000))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera 15 minutos.");
    }
    public synchronized void registrarFallo(String clave) {
        Instant ahora = Instant.now();
        intentos.entrySet().removeIf(e -> !e.getValue().vence().isAfter(ahora));
        Ventana v = intentos.get(clave);
        if (v == null && intentos.size() >= 10000) return;
        intentos.put(clave, new Ventana(v == null ? 1 : v.cantidad()+1, v == null ? ahora.plusSeconds(900) : v.vence()));
    }
    public synchronized void reiniciar(String clave) {
        intentos.remove(clave);
    }
}
