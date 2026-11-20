package br.com.nutriflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bloqueio da agenda (RF-009). Sem horario inicial/final = dia inteiro.
 * Recorrente = repete toda semana, no mesmo dia da semana, a partir da data informada.
 */
@Entity
public class Bloqueio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "Informe a data")
    private LocalDate data;
    private LocalTime inicio;
    private LocalTime fim;
    private boolean recorrente;
    private String motivo;

    /** Indica se este bloqueio atinge o horario de 60 minutos que comeca em "hora" no dia "dia". */
    public boolean atinge(LocalDate dia, LocalTime hora) {
        boolean mesmoDia = recorrente
                ? !dia.isBefore(data) && dia.getDayOfWeek() == data.getDayOfWeek()
                : dia.equals(data);
        if (!mesmoDia) return false;
        if (inicio == null || fim == null) return true;
        return hora.isBefore(fim) && hora.plusMinutes(60).isAfter(inicio);
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalTime getInicio() { return inicio; }
    public void setInicio(LocalTime inicio) { this.inicio = inicio; }
    public LocalTime getFim() { return fim; }
    public void setFim(LocalTime fim) { this.fim = fim; }
    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
