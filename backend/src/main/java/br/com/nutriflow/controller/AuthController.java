package br.com.nutriflow.controller;

import br.com.nutriflow.config.JwtService;
import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.Enums.Objetivo;
import br.com.nutriflow.model.Enums.Perfil;
import br.com.nutriflow.model.Enums.Sexo;
import br.com.nutriflow.model.Usuario;
import br.com.nutriflow.repository.Repositorios.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Telas publicas: login, cadastro de paciente e recuperacao de senha (RF-001 a RF-004). */
@RestController
@RequestMapping("/api")
public class AuthController {
    private static final int MINUTOS_TOKEN_RECUPERACAO = 30;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Relogio relogio;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, Relogio relogio) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.relogio = relogio;
    }

    public record LoginReq(@NotBlank(message = "Informe o e-mail") String email,
                           @NotBlank(message = "Informe a senha") String senha,
                           Perfil perfil) {}

    public record CadastroReq(@NotBlank(message = "Informe o nome completo") String nome,
                              @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
                              @NotBlank(message = "Informe o telefone") String telefone,
                              @NotNull(message = "Informe a idade") @Min(value = 1, message = "Idade inválida")
                              @Max(value = 120, message = "Idade inválida") Integer idade,
                              @NotNull(message = "Informe o sexo") Sexo sexo,
                              @NotNull(message = "Informe o objetivo nutricional") Objetivo objetivo,
                              @NotBlank(message = "Informe a senha")
                              @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String senha) {}

    public record RecuperarReq(@NotBlank(message = "Informe o e-mail") String email) {}

    public record RedefinirReq(@NotBlank(message = "Informe o código") String token,
                               @NotBlank(message = "Informe a nova senha")
                               @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String novaSenha) {}

    /** Informacoes publicas do ambiente, usadas pelo frontend. */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("demo", relogio.isDemo());
        m.put("hoje", relogio.hoje());
        return m;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        if (u == null || !encoder.matches(req.senha(), u.getSenhaHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
        if (req.perfil() != null && req.perfil() != u.getPerfil())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, req.perfil() == Perfil.PACIENTE
                    ? "Esta conta não é de paciente. Selecione \"Sou nutricionista\"."
                    : "Esta conta não é de nutricionista. Selecione \"Sou paciente\".");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", jwt.gerar(u));
        m.put("nome", u.getNome());
        m.put("perfil", u.getPerfil().name());
        return m;
    }

    /** Cadastro publico: cria sempre uma conta de PACIENTE (RN-002). */
    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> cadastro(@Valid @RequestBody CadastroReq req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.findByEmailIgnoreCase(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail");
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setTelefone(req.telefone().trim());
        u.setIdade(req.idade());
        u.setSexo(req.sexo());
        u.setObjetivo(req.objetivo());
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPerfil(Perfil.PACIENTE);
        usuarios.save(u);
        return Map.of("message", "Conta criada. Faça o login para continuar.");
    }

    /**
     * Gera um codigo de recuperacao valido por 30 minutos. Como o MVP nao envia e-mails,
     * o codigo so e mostrado na tela no ambiente de demonstracao; fora dele, aparece apenas no log do servidor.
     */
    @PostMapping("/auth/recuperar")
    public Map<String, Object> recuperar(@Valid @RequestBody RecuperarReq req) {
        Map<String, Object> m = new LinkedHashMap<>();
        // A resposta e a mesma exista ou nao a conta, para nao revelar quais e-mails estao cadastrados.
        m.put("message", "Se o e-mail estiver cadastrado, um código de recuperação foi gerado.");
        usuarios.findByEmailIgnoreCase(req.email().trim()).ifPresent(u -> {
            String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            u.setTokenRecuperacao(token);
            u.setTokenExpiraEm(LocalDateTime.now().plusMinutes(MINUTOS_TOKEN_RECUPERACAO));
            usuarios.save(u);
            if (relogio.isDemo()) m.put("codigoDemo", token);
            else System.out.println("[NutriFlow] Código de recuperação de " + u.getEmail() + ": " + token);
        });
        return m;
    }

    @PostMapping("/auth/redefinir")
    public Map<String, Object> redefinir(@Valid @RequestBody RedefinirReq req) {
        Usuario u = usuarios.findByTokenRecuperacao(req.token().trim().toUpperCase())
                .filter(x -> x.getTokenExpiraEm() != null && x.getTokenExpiraEm().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou vencido"));
        u.setSenhaHash(encoder.encode(req.novaSenha()));
        u.setTokenRecuperacao(null);
        u.setTokenExpiraEm(null);
        usuarios.save(u);
        return Map.of("message", "Senha alterada. Faça o login com a nova senha.");
    }
}
