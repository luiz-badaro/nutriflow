package br.com.nutriflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Analise gerada pela IA para um mes. Fica guardada e nao se atualiza sozinha (RN-032). */
@Entity
public class RelatorioIA {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int ano;
    private int mes;
    private LocalDateTime geradoEm;
    /** JSON com as 9 secoes devolvidas pelo Gemini. */
    @Column(columnDefinition = "LONGTEXT")
    private String conteudo;

    public RelatorioIA() {}
    public RelatorioIA(int ano, int mes, LocalDateTime geradoEm, String conteudo) {
        this.ano = ano;
        this.mes = mes;
        this.geradoEm = geradoEm;
        this.conteudo = conteudo;
    }

    public Long getId() { return id; }
    public int getAno() { return ano; }
    public int getMes() { return mes; }
    public LocalDateTime getGeradoEm() { return geradoEm; }
    public String getConteudo() { return conteudo; }
}
