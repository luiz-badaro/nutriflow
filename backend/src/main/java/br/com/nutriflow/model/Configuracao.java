package br.com.nutriflow.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Configuracao unica do consultorio (sempre o registro de id 1). */
@Entity
public class Configuracao {
    public static final Long ID = 1L;

    @Id
    private Long id = ID;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorConsulta = new BigDecimal("150.00");
    /** Usada apenas no ambiente de demonstracao (RF-038). */
    private LocalDate dataSimulada;

    public Long getId() { return id; }
    public BigDecimal getValorConsulta() { return valorConsulta; }
    public void setValorConsulta(BigDecimal valorConsulta) { this.valorConsulta = valorConsulta; }
    public LocalDate getDataSimulada() { return dataSimulada; }
    public void setDataSimulada(LocalDate dataSimulada) { this.dataSimulada = dataSimulada; }
}
