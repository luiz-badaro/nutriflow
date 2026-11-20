package br.com.nutriflow.model;

import br.com.nutriflow.model.Enums.Objetivo;
import br.com.nutriflow.model.Enums.Perfil;
import br.com.nutriflow.model.Enums.Sexo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Conta de acesso. Pacientes e o nutricionista ficam na mesma tabela, separados pelo perfil. */
@Entity
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Perfil perfil;
    private String telefone;
    private Integer idade;
    @Enumerated(EnumType.STRING)
    private Sexo sexo;
    @Enumerated(EnumType.STRING)
    private Objetivo objetivo;
    private String tokenRecuperacao;
    private LocalDateTime tokenExpiraEm;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public Objetivo getObjetivo() { return objetivo; }
    public void setObjetivo(Objetivo objetivo) { this.objetivo = objetivo; }
    public String getTokenRecuperacao() { return tokenRecuperacao; }
    public void setTokenRecuperacao(String tokenRecuperacao) { this.tokenRecuperacao = tokenRecuperacao; }
    public LocalDateTime getTokenExpiraEm() { return tokenExpiraEm; }
    public void setTokenExpiraEm(LocalDateTime tokenExpiraEm) { this.tokenExpiraEm = tokenExpiraEm; }
}
