package br.com.nutriflow.model;

import br.com.nutriflow.model.Enums.CategoriaDespesa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Despesa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Informe a descrição")
    private String descricao;
    @NotNull(message = "Informe a categoria") @Enumerated(EnumType.STRING)
    private CategoriaDespesa categoria;
    @NotNull(message = "Informe o valor") @Positive(message = "O valor deve ser maior que zero")
    @Column(precision = 10, scale = 2)
    private BigDecimal valor;
    @NotNull(message = "Informe a data")
    private LocalDate data;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public CategoriaDespesa getCategoria() { return categoria; }
    public void setCategoria(CategoriaDespesa categoria) { this.categoria = categoria; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
