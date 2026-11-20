package br.com.nutriflow.service;

import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.*;
import br.com.nutriflow.model.Enums.Origem;
import br.com.nutriflow.model.Enums.Perfil;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import br.com.nutriflow.repository.Repositorios.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Todos os calculos do sistema ficam aqui, feitos em Java a partir dos registros gravados (RNF-014).
 * A IA nunca calcula nada: ela recebe estes numeros prontos.
 * Indicador que nao pode ser calculado (divisao por zero) volta como null, nunca como zero (RN-025, RN-026).
 */
@Service
public class IndicadoresService {
    public static final int DIAS_ALERTA_RETENCAO = 45;
    public static final int DIAS_META_RETENCAO = 60;
    private static final int MESES_EVOLUCAO = 6;

    private final AgendamentoRepository agendamentos;
    private final ReceitaRepository receitas;
    private final DespesaRepository despesas;
    private final InvestimentoRepository investimentos;
    private final PeriodoRepository periodos;
    private final BloqueioRepository bloqueios;
    private final UsuarioRepository usuarios;
    private final Relogio relogio;

    public IndicadoresService(AgendamentoRepository agendamentos, ReceitaRepository receitas, DespesaRepository despesas,
                              InvestimentoRepository investimentos, PeriodoRepository periodos, BloqueioRepository bloqueios,
                              UsuarioRepository usuarios, Relogio relogio) {
        this.agendamentos = agendamentos;
        this.receitas = receitas;
        this.despesas = despesas;
        this.investimentos = investimentos;
        this.periodos = periodos;
        this.bloqueios = bloqueios;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------ Formulas

    /** CPA = investimento do canal / conversoes do canal. Sem conversoes: nao calculavel (RN-025). */
    public static BigDecimal cpa(BigDecimal investimento, long conversoes) {
        if (conversoes == 0) return null;
        return investimento.divide(BigDecimal.valueOf(conversoes), 2, RoundingMode.HALF_UP);
    }

    /** ROI (%) = ((receita atribuida - investimento) / investimento) x 100. Investimento zero: nao calculavel (RN-026). */
    public static BigDecimal roi(BigDecimal receitaAtribuida, BigDecimal investimento) {
        if (investimento.signum() == 0) return null;
        return receitaAtribuida.subtract(investimento).multiply(BigDecimal.valueOf(100))
                .divide(investimento, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somar(Collection<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentual(long parte, long total) {
        if (total == 0) return null;
        return BigDecimal.valueOf(parte * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ Financeiro (RF-019)

    private Map<String, Object> totaisFinanceiros(YearMonth mes) {
        BigDecimal receita = somar(receitas.findByDataBetweenOrderByDataDesc(mes.atDay(1), mes.atEndOfMonth())
                .stream().map(Receita::getValor).toList());
        BigDecimal despesa = somar(despesas.findByDataBetweenOrderByDataDesc(mes.atDay(1), mes.atEndOfMonth())
                .stream().map(Despesa::getValor).toList());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mes", mes.toString());
        m.put("receita", receita);
        m.put("despesas", despesa);
        m.put("saldo", receita.subtract(despesa)); // RN-016
        return m;
    }

    private static boolean temMovimento(Map<String, Object> totais) {
        return ((BigDecimal) totais.get("receita")).signum() != 0 || ((BigDecimal) totais.get("despesas")).signum() != 0;
    }

    public Map<String, Object> financeiro(YearMonth mes) {
        Map<String, Object> m = totaisFinanceiros(mes);
        Map<String, Object> anterior = totaisFinanceiros(mes.minusMonths(1));
        m.put("anterior", temMovimento(anterior) ? anterior : null); // null = sem dados para comparar
        List<Map<String, Object>> evolucao = new ArrayList<>();
        for (int i = MESES_EVOLUCAO - 1; i >= 0; i--) evolucao.add(totaisFinanceiros(mes.minusMonths(i)));
        m.put("evolucao", evolucao);
        return m;
    }

    // ------------------------------------------------------------------ Marketing (RF-021 a RF-024)

    private List<Agendamento> confirmados(YearMonth mes) {
        return agendamentos.findByDataBetween(mes.atDay(1), mes.atEndOfMonth()).stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO).toList();
    }

    private Map<String, Object> linhaMarketing(String nome, BigDecimal investimento, List<Agendamento> conversoes) {
        BigDecimal receitaAtribuida = somar(conversoes.stream().map(Agendamento::getValor).toList());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("canal", nome);
        m.put("investimento", investimento);
        m.put("conversoes", conversoes.size());
        m.put("receitaAtribuida", receitaAtribuida);
        m.put("cpa", cpa(investimento, conversoes.size()));
        m.put("roi", roi(receitaAtribuida, investimento));
        return m;
    }

    private Map<String, Object> resumoMarketing(YearMonth mes) {
        List<Agendamento> confirmados = confirmados(mes);
        List<InvestimentoMarketing> invs = investimentos.findByAnoAndMes(mes.getYear(), mes.getMonthValue());

        List<Map<String, Object>> canais = new ArrayList<>();
        List<Agendamento> todasConversoes = new ArrayList<>();
        for (Origem canal : Origem.values()) {
            if (!canal.isCanalDeMarketing()) continue; // Retorno nunca e canal (RN-019, RN-022)
            List<Agendamento> doCanal = confirmados.stream().filter(a -> a.getOrigem() == canal).toList();
            BigDecimal investido = somar(invs.stream().filter(i -> i.getCanal() == canal).map(InvestimentoMarketing::getValor).toList());
            canais.add(linhaMarketing(canal.name(), investido, doCanal));
            todasConversoes.addAll(doCanal);
        }
        BigDecimal investimentoTotal = somar(invs.stream().map(InvestimentoMarketing::getValor).toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mes", mes.toString());
        m.put("totais", linhaMarketing("TOTAL", investimentoTotal, todasConversoes));
        m.put("canais", canais);
        m.put("consultasConfirmadas", confirmados.size());
        m.put("consultasRetorno", confirmados.size() - todasConversoes.size());
        return m;
    }

    public Map<String, Object> marketing(YearMonth mes) {
        Map<String, Object> m = resumoMarketing(mes);
        Map<String, Object> anterior = resumoMarketing(mes.minusMonths(1));
        boolean temAnterior = (int) anterior.get("consultasConfirmadas") > 0
                || ((BigDecimal) ((Map<?, ?>) anterior.get("totais")).get("investimento")).signum() != 0;
        m.put("anterior", temAnterior ? anterior.get("totais") : null);
        m.put("perfil", perfilDoPublico(mes));
        return m;
    }

    private static String faixaEtaria(Integer idade) {
        if (idade == null) return "Não informado";
        if (idade < 18) return "Até 17";
        if (idade < 30) return "18 a 29";
        if (idade < 45) return "30 a 44";
        if (idade < 60) return "45 a 59";
        return "60 ou mais";
    }

    private static Map<String, Long> contar(List<Usuario> pessoas, Function<Usuario, Object> campo) {
        return pessoas.stream().collect(Collectors.groupingBy(
                u -> campo.apply(u) == null ? "Não informado" : campo.apply(u).toString(), TreeMap::new, Collectors.counting()));
    }

    private static Map<String, Object> distribuicoes(List<Usuario> pessoas) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", pessoas.size());
        m.put("sexo", contar(pessoas, Usuario::getSexo));
        m.put("objetivo", contar(pessoas, Usuario::getObjetivo));
        m.put("faixaEtaria", contar(pessoas, u -> faixaEtaria(u.getIdade())));
        return m;
    }

    /**
     * Perfil geral = pacientes cadastrados. Perfil por canal = consultas confirmadas do canal no mes
     * (conta consultas, nao pacientes unicos; Retorno fica fora).
     */
    private Map<String, Object> perfilDoPublico(YearMonth mes) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("geral", distribuicoes(usuarios.findByPerfilOrderByNome(Perfil.PACIENTE)));
        Map<String, Object> porCanal = new LinkedHashMap<>();
        List<Agendamento> confirmados = confirmados(mes);
        for (Origem canal : Origem.values()) {
            if (!canal.isCanalDeMarketing()) continue;
            porCanal.put(canal.name(), distribuicoes(
                    confirmados.stream().filter(a -> a.getOrigem() == canal).map(Agendamento::getPaciente).toList()));
        }
        m.put("porCanal", porCanal);
        return m;
    }

    // ------------------------------------------------------------------ Agenda (RF-029)

    /** Ocupacao = horarios com solicitacao pendente ou confirmada / horarios de atendimento do mes. */
    public Map<String, Object> agenda(YearMonth mes) {
        List<PeriodoAtendimento> ps = periodos.findAll();
        List<Bloqueio> bs = bloqueios.findAll();
        List<Agendamento> doMes = agendamentos.findByDataBetween(mes.atDay(1), mes.atEndOfMonth());
        Set<String> ocupados = doMes.stream().filter(a -> a.getStatus() != StatusAgendamento.NEGADO)
                .map(a -> a.getData() + "T" + a.getHora()).collect(Collectors.toSet());

        Map<DayOfWeek, long[]> porDia = new TreeMap<>();   // [total, ocupados]
        Map<LocalTime, long[]> porHora = new TreeMap<>();
        long total = 0;
        for (LocalDate dia = mes.atDay(1); !dia.isAfter(mes.atEndOfMonth()); dia = dia.plusDays(1)) {
            Set<LocalTime> horas = new TreeSet<>(AgendaService.horariosDeAtendimento(dia, ps, bs));
            // Um horario ocupado sempre conta, mesmo que a disponibilidade tenha mudado depois.
            for (Agendamento a : doMes)
                if (a.getData().equals(dia) && a.getStatus() != StatusAgendamento.NEGADO) horas.add(a.getHora());
            for (LocalTime hora : horas) {
                boolean ocupado = ocupados.contains(dia + "T" + hora);
                total++;
                long[] d = porDia.computeIfAbsent(dia.getDayOfWeek(), k -> new long[2]);
                long[] h = porHora.computeIfAbsent(hora, k -> new long[2]);
                d[0]++; h[0]++;
                if (ocupado) { d[1]++; h[1]++; }
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mes", mes.toString());
        m.put("consultasConfirmadas", doMes.stream().filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO).count());
        m.put("solicitacoesPendentes", doMes.stream().filter(a -> a.getStatus() == StatusAgendamento.PENDENTE).count());
        m.put("horariosTotais", total);
        m.put("horariosOcupados", ocupados.size());
        m.put("horariosDisponiveis", total - ocupados.size());
        m.put("taxaOcupacao", percentual(ocupados.size(), total));
        List<Map<String, Object>> dias = faixas(porDia);
        List<Map<String, Object>> horas = faixas(porHora);
        m.put("porDiaSemana", dias);
        m.put("porHorario", horas);
        m.put("diaMenorProcura", menorProcura(dias));
        m.put("horarioMenorProcura", menorProcura(horas));
        return m;
    }

    private static List<Map<String, Object>> faixas(Map<?, long[]> contagem) {
        List<Map<String, Object>> lista = new ArrayList<>();
        contagem.forEach((chave, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", chave.toString());
            m.put("total", v[0]);
            m.put("ocupados", v[1]);
            m.put("taxa", percentual(v[1], v[0]));
            lista.add(m);
        });
        return lista;
    }

    private static Object menorProcura(List<Map<String, Object>> faixas) {
        return faixas.stream().filter(f -> f.get("taxa") != null)
                .min(Comparator.comparing(f -> (BigDecimal) f.get("taxa"))).map(f -> f.get("nome")).orElse(null);
    }

    // ------------------------------------------------------------------ Retencao (RF-025, RN-029)

    /** Pacientes com mais de 45 dias desde a ultima consulta confirmada ja realizada, de qualquer origem. */
    public Map<String, Object> retencao() {
        LocalDate hoje = relogio.hoje();
        Map<Long, LocalDate> ultimaConsulta = new HashMap<>();
        Map<Long, Usuario> pacientes = new HashMap<>();
        for (Agendamento a : agendamentos.findByStatus(StatusAgendamento.CONFIRMADO)) {
            if (a.getData().isAfter(hoje)) continue; // consulta futura ainda nao aconteceu
            pacientes.put(a.getPaciente().getId(), a.getPaciente());
            ultimaConsulta.merge(a.getPaciente().getId(), a.getData(), (x, y) -> x.isAfter(y) ? x : y);
        }
        List<Map<String, Object>> alertas = new ArrayList<>();
        ultimaConsulta.forEach((pacienteId, data) -> {
            Usuario paciente = pacientes.get(pacienteId);
            long dias = ChronoUnit.DAYS.between(data, hoje);
            if (dias <= DIAS_ALERTA_RETENCAO) return;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pacienteId", paciente.getId());
            m.put("nome", paciente.getNome());
            m.put("telefone", paciente.getTelefone());
            m.put("ultimaConsulta", data);
            m.put("diasSemRetorno", dias);
            m.put("acimaDaMeta", dias > DIAS_META_RETENCAO); // passou dos 60 dias que se quer evitar (RN-030)
            alertas.add(m);
        });
        alertas.sort(Comparator.comparing(m -> -((Long) m.get("diasSemRetorno"))));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dataReferencia", hoje);
        m.put("pacientesComHistorico", ultimaConsulta.size());
        m.put("totalAlertas", alertas.size());
        m.put("acimaDe60Dias", alertas.stream().filter(a -> (boolean) a.get("acimaDaMeta")).count());
        m.put("alertas", alertas);
        return m;
    }

    // ------------------------------------------------------------------ Tudo junto (dashboard e IA)

    public Map<String, Object> tudo(YearMonth mes) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mes", mes.toString());
        m.put("financeiro", financeiro(mes));
        m.put("marketing", marketing(mes));
        m.put("agenda", agenda(mes));
        m.put("retencao", retencao());
        return m;
    }
}
