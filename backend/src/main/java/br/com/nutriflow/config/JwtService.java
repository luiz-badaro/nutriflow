package br.com.nutriflow.config;

import br.com.nutriflow.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Emite e valida os tokens de acesso (validade de 8 horas). */
@Component
public class JwtService {
    private static final long VALIDADE_MS = 8 * 60 * 60 * 1000L;
    private final SecretKey chave;

    public JwtService(@Value("${nutriflow.jwt.secret:}") String segredo) {
        // Sem segredo configurado, gera uma chave aleatoria: os tokens deixam de valer ao reiniciar o servidor.
        this.chave = segredo == null || segredo.length() < 32
                ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerar(Usuario usuario) {
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("perfil", usuario.getPerfil().name())
                .expiration(new Date(System.currentTimeMillis() + VALIDADE_MS))
                .signWith(chave)
                .compact();
    }

    /** Devolve os dados do token ou lanca excecao se ele for invalido ou estiver vencido. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
    }
}
