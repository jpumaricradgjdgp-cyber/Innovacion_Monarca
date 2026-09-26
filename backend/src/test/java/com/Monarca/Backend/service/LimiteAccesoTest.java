package com.Monarca.Backend.service;
import com.Monarca.Backend.security.LimiteAcceso;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class LimiteAccesoTest {
    @Test void limitaPorClaveSinBloquearOtrasCuentas(){var limite=new LimiteAcceso();limite.comprobar("uno",2);limite.comprobar("uno",2);
        assertEquals(429,assertThrows(ResponseStatusException.class,()->limite.comprobar("uno",2)).getStatusCode().value());assertDoesNotThrow(()->limite.comprobar("otro",2));}
}
