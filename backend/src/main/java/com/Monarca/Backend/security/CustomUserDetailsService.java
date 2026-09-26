package com.Monarca.Backend.security;

import com.Monarca.Backend.model.Usuario;
import com.Monarca.Backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado: " + correo
                        )
                );

        return new UsuarioAutenticado(usuario);
    }

    public static final class UsuarioAutenticado extends User {
        private final String versionCredenciales;
        private final String nombre;

        private UsuarioAutenticado(Usuario usuario) {
            super(usuario.getCorreo(), usuario.getPassword(), Boolean.TRUE.equals(usuario.getActivo()),
                    true, true, true, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            "ROLE_" + usuario.getRol().getNombre())));
            // Se conserva solo la versión para el JWT; Spring puede borrar la contraseña.
            versionCredenciales = com.Monarca.Backend.service.PasswordRecoveryService.hash(usuario.getPassword());
            nombre = usuario.getNombres();
        }

        public String getVersionCredenciales() { return versionCredenciales; }
        public String getNombre() { return nombre; }
    }
}
