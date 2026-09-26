package com.Monarca.Backend.controller;

import com.Monarca.Backend.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginLimitesTest {
    MockMvc mvc;
    @BeforeEach void preparar() {
        var controller = new AuthController();
        var limites = new LimiteAcceso();
        var manager = mock(AuthenticationManager.class);
        var jwt = mock(JwtUtil.class);
        when(manager.authenticate(any())).thenAnswer(invocation -> {
            var solicitud = (org.springframework.security.core.Authentication) invocation.getArgument(0);
            if (!"correcta".equals(solicitud.getCredentials())) throw new BadCredentialsException("Incorrecta");
            var usuario = User.withUsername(solicitud.getName()).password("hash").roles("CLIENTE").build();
            return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        });
        when(jwt.generateToken(any())).thenReturn("token-de-prueba");
        ReflectionTestUtils.setField(controller, "limites", limites);
        ReflectionTestUtils.setField(controller, "authenticationManager", manager);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwt);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiErrores())
                .addFilters(new FiltroLimiteAcceso(limites)).build();
    }
    void login(String correo, String password, int esperado) throws Exception {
        mvc.perform(post("/api/auth/login").servletPath("/api/auth/login")
                .contentType("application/json").content("{\"email\":\""+correo+"\",\"password\":\""+password+"\"}"))
                .andExpect(status().is(esperado));
    }
    @Test void accesosCorrectosNoAgotanCuentaNiIp() throws Exception {
        for (int i=0;i<70;i++) login("cliente@example.com", "correcta", 200);
    }
    @Test void accesoCorrectoReiniciaFallosDeCuenta() throws Exception {
        for (int i=0;i<19;i++) login("cliente@example.com", "incorrecta", 401);
        login("cliente@example.com", "correcta", 200);
        for (int i=0;i<19;i++) login("cliente@example.com", "incorrecta", 401);
        login("cliente@example.com", "correcta", 200);
    }
    @Test void muchosFallosBloqueanSoloEsaCuenta() throws Exception {
        for (int i=0;i<20;i++) login("cliente@example.com", "incorrecta", 401);
        login("cliente@example.com", "incorrecta", 429);
        login("otro@example.com", "correcta", 200);
    }
    @Test void ipLimitaFallosRepartidosEntreCuentas() throws Exception {
        for (int i=0;i<50;i++) login("cliente"+i+"@example.com", "incorrecta", 401);
        login("otro@example.com", "incorrecta", 429);
    }
}
