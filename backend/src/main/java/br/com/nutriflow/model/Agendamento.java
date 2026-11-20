package br.com.nutriflow.model;

import br.com.nutriflow.model.Enums.Origem;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Usuario paciente;
    @Column(nullable = false)
    private LocalDate data;
    @Column(nullable = false)
    private LocalTime hora;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Origem origem;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusAgendamento status;
    /** Valor da consulta copiado no momento da solicitacao (RN-013). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    private LocalDateTime criadoEm;
    private LocalDateTime decididoEm;
    /**
     * "data + hora" enquanto o agendamento ocupa o horario (pendente ou confirmado) e nulo quando negado.
     * O indice unico faz o proprio banco impedir dois pacientes no mesmo horario (RNF-012).
     */
    @Column(unique = true)
    private String slotAtivo;
    /** Controle de concorrencia: impede confirmar/negar a mesma solicitacao duas vezes ao mesmo tempo. */
    @Version
    private Long versao;

    public Long getId() { return id; }
    public Usuario getPaciente() { return paciente; }
    public void setPaciente(Usuario paciente) { this.paciente = paciente; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Origem getOrigem() { return origem; }
    public void setOrigem(Origem origem) { this.origem = origem; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getDecididoEm() { return decididoEm; }
    public void setDecididoEm(LocalDateTime decididoEm) { this.decididoEm = decididoEm; }
    public String getSlotAtivo() { return slotAtivo; }
    public void setSlotAtivo(String slotAtivo) { this.slotAtivo = slotAtivo; }
}
