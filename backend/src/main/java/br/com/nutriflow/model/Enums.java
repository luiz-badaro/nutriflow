package br.com.nutriflow.model;

/** Enumeracoes do dominio, reunidas em um unico arquivo. */
public final class Enums {
    private Enums() {}

    public enum Perfil { PACIENTE, NUTRICIONISTA }

    public enum Sexo { FEMININO, MASCULINO, OUTRO }

    public enum Objetivo { EMAGRECIMENTO, GANHO_DE_MASSA, REEDUCACAO_ALIMENTAR, SAUDE, DESEMPENHO_ESPORTIVO, OUTRO }

    public enum StatusAgendamento { PENDENTE, CONFIRMADO, NEGADO }

    /** Origem informada em cada agendamento. RETORNO nao e canal de marketing (RN-019). */
    public enum Origem {
        INSTAGRAM, GOOGLE, INDICACAO, RETORNO, OUTRO;

        public boolean isCanalDeMarketing() { return this != RETORNO; }
    }

    public enum CategoriaDespesa { ALUGUEL, MATERIAIS, SOFTWARE, IMPOSTOS, PESSOAL, OUTROS }
}
