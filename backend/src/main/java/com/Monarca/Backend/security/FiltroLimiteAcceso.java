package com.Monarca.Backend.security;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
/** Se instala en Spring Security, después de CORS; no duplica el registro servlet. */
public class FiltroLimiteAcceso extends OncePerRequestFilter {
    private final LimiteAcceso limites;
    public FiltroLimiteAcceso(LimiteAcceso limites) { this.limites = limites; }
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String ruta = req.getServletPath();
        if ("POST".equals(req.getMethod()) && (ruta.equals("/api/auth/login") || ruta.equals("/api/auth/registro"))) {
            String clave = ruta + ":" + req.getRemoteAddr();
            boolean login = ruta.equals("/api/auth/login");
            try {
                if (login) limites.verificar(clave, 50);
                else limites.comprobar(clave, 15);
            }
            catch (org.springframework.web.server.ResponseStatusException e) {
                res.setStatus(429); res.setHeader("Retry-After", "900"); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Demasiados intentos. Espera 15 minutos.\"}"); return;
            }
            chain.doFilter(req,res);
            // Los accesos correctos no consumen el límite compartido de la IP.
            if (login && res.getStatus() == 401) limites.registrarFallo(clave);
            return;
        }
        chain.doFilter(req,res);
    }
}
