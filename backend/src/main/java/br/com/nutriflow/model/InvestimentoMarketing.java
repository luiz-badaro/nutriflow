package br.com.nutriflow.model;

import br.com.nutriflow.model.Enums.Origem;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Investimento manual em um canal, por mes de referencia (RF-020). */
@Entity
public class InvestimentoMarketing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "Informe o canal") @Enumerated(EnumType.STRING)
    private Origem canal;
    @NotNull(message = "Informe o ano")
    private Integer ano;
    @NotNull(message = "Informe o mês") @Min(1) @Max(12)
    private Integer mes;
    @NotNull(message = "Informe o valor") @Positive(message = "O valor deve ser maior que zero")
    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Origem getCanal() { return canal; }
    public void setCanal(Origem canal) { this.canal = canal; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
