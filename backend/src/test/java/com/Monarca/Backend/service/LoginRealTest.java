package com.Monarca.Backend.service;

import com.Monarca.Backend.model.*;
import com.Monarca.Backend.repository.UsuarioRepository;
import com.Monarca.Backend.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRealTest {
    @Test void generaTokenTrasBorrarCredencialesYLoInvalidaAlCambiarPassword() {
        var repository = mock(UsuarioRepository.class);
        var encoder = new BCryptPasswordEncoder();
        var usuario = new Usuario();
        var rol = new Rol(); rol.setNombre("CLIENTE");
        usuario.setRol(rol); usuario.setCorreo("cliente@example.test");
        usuario.setNombres("María José");
        usuario.setPassword(encoder.encode("PasswordDePrueba123!"));
        when(repository.findByCorreoIgnoreCase(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        var servicio = new CustomUserDetailsService(repository);
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(servicio); provider.setPasswordEncoder(encoder);
        var manager = new ProviderManager(provider);
        var autenticacion = manager.authenticate(new UsernamePasswordAuthenticationToken(usuario.getCorreo(), "PasswordDePrueba123!"));
        var principal = (UserDetails) autenticacion.getPrincipal();
        assertNull(principal.getPassword(), "Spring borra las credenciales tras autenticar");
        var jwt = new JwtUtil("clave-de-prueba-exclusiva-de-test-1234567890");
        var token = jwt.generateToken(principal);
        assertEquals("María José",jwt.extractClaim(token,c -> c.get("nombre",String.class)));
        assertTrue(jwt.validateToken(token, servicio.loadUserByUsername(usuario.getCorreo())));
        usuario.setPassword(encoder.encode("OtraPasswordDePrueba123!"));
        assertFalse(jwt.validateToken(token, servicio.loadUserByUsername(usuario.getCorreo())));
    }
}
