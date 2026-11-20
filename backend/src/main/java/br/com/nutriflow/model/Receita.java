package br.com.nutriflow.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Receita gerada automaticamente na confirmacao. No maximo uma por agendamento (RN-014). */
@Entity
public class Receita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private Agendamento agendamento;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    /** Data da consulta: e nela que a receita entra nos indicadores mensais. */
    @Column(nullable = false)
    private LocalDate data;

    public Receita() {}
    public Receita(Agendamento agendamento) {
        this.agendamento = agendamento;
        this.valor = agendamento.getValor();
        this.data = agendamento.getData();
    }

    public Long getId() { return id; }
    public Agendamento getAgendamento() { return agendamento; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
}
