package br.com.nutriflow.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Regras de acesso por perfil, sempre validadas no backend (RNF-008):
 * /api/auth/** e publico, /api/paciente/** so paciente, /api/admin/** so nutricionista.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/info", "/error").permitAll()
                .requestMatchers("/api/paciente/**").hasRole("PACIENTE")
                .requestMatchers("/api/admin/**").hasRole("NUTRICIONISTA")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Le o cabecalho "Authorization: Bearer token" e identifica o usuario (o id vira o "principal"). */
    static class FiltroJwt extends OncePerRequestFilter {
        private final JwtService jwt;

        FiltroJwt(JwtService jwt) { this.jwt = jwt; }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String cabecalho = req.getHeader("Authorization");
            if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
                try {
                    Claims dados = jwt.validar(cabecalho.substring(7));
                    Long usuarioId = Long.valueOf(dados.getSubject());
                    var autoridade = new SimpleGrantedAuthority("ROLE_" + dados.get("perfil", String.class));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(usuarioId, null, List.of(autoridade)));
                } catch (Exception tokenInvalido) {
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(req, res);
        }
    }
}
