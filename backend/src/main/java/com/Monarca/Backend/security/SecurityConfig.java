package com.Monarca.Backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @org.springframework.beans.factory.annotation.Value("${monarca.cors.origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:5501,http://127.0.0.1:5501}")
    private String origenes;
    @Bean public LimiteAcceso limiteAcceso() { return new LimiteAcceso(); }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                // CORS
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // API REST + JWT -> sin CSRF
                .csrf(csrf -> csrf.disable())

                // Sesiones desactivadas
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Reglas de autorización
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // Preflight CORS
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // =====================================
                        // AUTENTICACIÓN PÚBLICA
                        // =====================================

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // =====================================
                        // CATÁLOGO PÚBLICO
                        // =====================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/productos",
                                "/api/productos/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categorias",
                                "/api/categorias/**"
                        ).permitAll()

                        // =====================================
                        // ADMINISTRACIÓN DE PRODUCTOS
                        // =====================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/productos",
                                "/api/productos/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/productos",
                                "/api/productos/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/productos",
                                "/api/productos/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // =====================================
                        // PEDIDOS
                        // =====================================

                        // Cliente o administrador autenticado
                        // puede realizar una compra
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/pedidos/procesar"
                        ).authenticated()

                        // Solo administrador ve todos los pedidos


                        // =====================================
                        // ERRORES
                        // =====================================

                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Todo lo demás requiere login
                        .anyRequest()
                        .authenticated()
                )

                // Provider de usuario + BCrypt
                .authenticationProvider(
                        authenticationProvider
                )

                // JWT antes del filtro estándar
                .addFilterBefore(new FiltroLimiteAcceso(limiteAcceso()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                java.util.Arrays.stream(origenes.split(",")).map(String::trim).toList()
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT", "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}