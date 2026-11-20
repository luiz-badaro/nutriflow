package br.com.nutriflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Um periodo de atendimento em um dia da semana. Varios periodos por dia sao permitidos (RF-007). */
@Entity
public class PeriodoAtendimento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @Enumerated(EnumType.STRING)
    private DayOfWeek diaSemana;
    @NotNull
    private LocalTime inicio;
    @NotNull
    private LocalTime fim;

    public PeriodoAtendimento() {}
    public PeriodoAtendimento(DayOfWeek diaSemana, LocalTime inicio, LocalTime fim) {
        this.diaSemana = diaSemana;
        this.inicio = inicio;
        this.fim = fim;
    }

    public Long getId() { return id; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getInicio() { return inicio; }
    public void setInicio(LocalTime inicio) { this.inicio = inicio; }
    public LocalTime getFim() { return fim; }
    public void setFim(LocalTime fim) { this.fim = fim; }
}
