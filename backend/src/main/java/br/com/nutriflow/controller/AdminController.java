package br.com.nutriflow.controller;

import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.*;
import br.com.nutriflow.model.Enums.Origem;
import br.com.nutriflow.model.Enums.Perfil;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import br.com.nutriflow.repository.Repositorios.*;
import br.com.nutriflow.service.AgendaService;
import br.com.nutriflow.service.AgendamentoService;
import br.com.nutriflow.service.IaService;
import br.com.nutriflow.service.IndicadoresService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Ambiente do nutricionista: as 8 abas do dashboard. Todo /api/admin/** exige o perfil NUTRICIONISTA. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AgendaService agenda;
    private final AgendamentoService agendamentoService;
    private final IndicadoresService indicadores;
    private final IaService ia;
    private final Relogio relogio;
    private final AgendamentoRepository agendamentos;
    private final UsuarioRepository usuarios;
    private final ReceitaRepository receitas;
    private final DespesaRepository despesas;
    private final InvestimentoRepository investimentos;
    private final ConfiguracaoRepository configuracoes;

    public AdminController(AgendaService agenda, AgendamentoService agendamentoService, IndicadoresService indicadores,
                           IaService ia, Relogio relogio, AgendamentoRepository agendamentos, UsuarioRepository usuarios,
                           ReceitaRepository receitas, DespesaRepository despesas, InvestimentoRepository investimentos,
                           ConfiguracaoRepository configuracoes) {
        this.agenda = agenda;
        this.agendamentoService = agendamentoService;
        this.indicadores = indicadores;
        this.ia = ia;
        this.relogio = relogio;
        this.agendamentos = agendamentos;
        this.usuarios = usuarios;
        this.receitas = receitas;
        this.despesas = despesas;
        this.investimentos = investimentos;
        this.configuracoes = configuracoes;
    }

    private Configuracao configuracao() {
        return configuracoes.findById(Configuracao.ID).orElseGet(() -> configuracoes.save(new Configuracao()));
    }

    // ------------------------------------------------------------------ Aba 1: Visao Geral

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam int ano, @RequestParam int mes) {
        Map<String, Object> m = indicadores.tudo(YearMonth.of(ano, mes));
        m.put("ultimoRelatorio", ia.listar(ano, mes).stream().findFirst().orElse(null));
        return m;
    }

    // ------------------------------------------------------------------ Aba 2: Agenda

    @GetMapping("/agenda/dia")
    public List<Map<String, Object>> agendaDoDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.agendaDoDia(data);
    }

    @GetMapping("/agendamentos")
    public List<Map<String, Object>> solicitacoes(@RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agendamentos.findAllByOrderByDataDescHoraDesc().stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> data == null || a.getData().equals(data))
                .map(a -> AgendamentoService.paraJson(a, true)).toList();
    }

    @PostMapping("/agendamentos/{id}/confirmar")
    public Map<String, Object> confirmar(@PathVariable Long id) {
        return AgendamentoService.paraJson(agendamentoService.decidir(id, true), true);
    }

    @PostMapping("/agendamentos/{id}/negar")
    public Map<String, Object> negar(@PathVariable Long id) {
        return AgendamentoService.paraJson(agendamentoService.decidir(id, false), true);
    }

    @GetMapping("/disponibilidade")
    public List<PeriodoAtendimento> disponibilidade() {
        return agenda.listarDisponibilidade();
    }

    @PutMapping("/disponibilidade")
    public List<PeriodoAtendimento> salvarDisponibilidade(@RequestBody List<PeriodoAtendimento> periodos) {
        return agenda.salvarDisponibilidade(periodos);
    }

    @GetMapping("/bloqueios")
    public List<Bloqueio> bloqueios() {
        return agenda.listarBloqueios();
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public Bloqueio criarBloqueio(@Valid @RequestBody Bloqueio bloqueio) {
        return agenda.criarBloqueio(bloqueio);
    }

    @DeleteMapping("/bloqueios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerBloqueio(@PathVariable Long id) {
        agenda.removerBloqueio(id);
    }

    // ------------------------------------------------------------------ Aba 3: Pacientes

    private static Map<String, Object> pacienteJson(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nome", u.getNome());
        m.put("email", u.getEmail());
        m.put("telefone", u.getTelefone());
        m.put("idade", u.getIdade());
        m.put("sexo", u.getSexo());
        m.put("objetivo", u.getObjetivo());
        return m;
    }

    @GetMapping("/pacientes")
    public List<Map<String, Object>> pacientes() {
        return usuarios.findByPerfilOrderByNome(Perfil.PACIENTE).stream().map(AdminController::pacienteJson).toList();
    }

    @GetMapping("/pacientes/{id}/historico")
    public Map<String, Object> historico(@PathVariable Long id) {
        Usuario u = usuarios.findById(id).filter(x -> x.getPerfil() == Perfil.PACIENTE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));
        Map<String, Object> m = pacienteJson(u);
        m.put("agendamentos", agendamentos.findByPacienteIdOrderByDataDescHoraDesc(id).stream()
                .map(a -> AgendamentoService.paraJson(a, true)).toList());
        return m;
    }

    // ------------------------------------------------------------------ Aba 4: Financeiro

    @GetMapping("/financeiro/resumo")
    public Map<String, Object> financeiro(@RequestParam int ano, @RequestParam int mes) {
        return indicadores.financeiro(YearMonth.of(ano, mes));
    }

    @GetMapping("/receitas")
    public List<Map<String, Object>> receitas(@RequestParam int ano, @RequestParam int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        return receitas.findByDataBetweenOrderByDataDesc(ym.atDay(1), ym.atEndOfMonth()).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("paciente", r.getAgendamento().getPaciente().getNome());
            m.put("data", r.getData());
            m.put("hora", r.getAgendamento().getHora());
            m.put("valor", r.getValor());
            m.put("origem", r.getAgendamento().getOrigem().name());
            return m;
        }).toList();
    }

    @GetMapping("/despesas")
    public List<Despesa> despesas(@RequestParam int ano, @RequestParam int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        return despesas.findByDataBetweenOrderByDataDesc(ym.atDay(1), ym.atEndOfMonth());
    }

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public Despesa criarDespesa(@Valid @RequestBody Despesa despesa) {
        despesa.setId(null);
        return despesas.save(despesa);
    }

    @PutMapping("/despesas/{id}")
    public Despesa editarDespesa(@PathVariable Long id, @Valid @RequestBody Despesa despesa) {
        if (!despesas.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada");
        despesa.setId(id);
        return despesas.save(despesa);
    }

    @DeleteMapping("/despesas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerDespesa(@PathVariable Long id) {
        despesas.deleteById(id);
    }

    public record ValorReq(BigDecimal valor) {}

    @GetMapping("/valor-consulta")
    public Map<String, Object> valorConsulta() {
        return Map.of("valor", configuracao().getValorConsulta());
    }

    /** Muda o valor padrao. Agendamentos ja criados mantem o valor que tinham (RN-013). */
    @PutMapping("/valor-consulta")
    public Map<String, Object> salvarValorConsulta(@RequestBody ValorReq req) {
        if (req.valor() == null || req.valor().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um valor maior que zero");
        Configuracao c = configuracao();
        c.setValorConsulta(req.valor());
        configuracoes.save(c);
        return Map.of("valor", c.getValorConsulta());
    }

    // ------------------------------------------------------------------ Aba 5: Marketing

    @GetMapping("/marketing/resumo")
    public Map<String, Object> marketing(@RequestParam int ano, @RequestParam int mes) {
        return indicadores.marketing(YearMonth.of(ano, mes));
    }

    @GetMapping("/investimentos")
    public List<InvestimentoMarketing> investimentos(@RequestParam int ano, @RequestParam int mes) {
        return investimentos.findByAnoAndMes(ano, mes);
    }

    private void validarCanal(InvestimentoMarketing inv) {
        if (inv.getCanal() == Origem.RETORNO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retorno não é um canal de marketing");
    }

    @PostMapping("/investimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestimentoMarketing criarInvestimento(@Valid @RequestBody InvestimentoMarketing inv) {
        validarCanal(inv);
        inv.setId(null);
        return investimentos.save(inv);
    }

    @PutMapping("/investimentos/{id}")
    public InvestimentoMarketing editarInvestimento(@PathVariable Long id, @Valid @RequestBody InvestimentoMarketing inv) {
        validarCanal(inv);
        if (!investimentos.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Investimento não encontrado");
        inv.setId(id);
        return investimentos.save(inv);
    }

    @DeleteMapping("/investimentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerInvestimento(@PathVariable Long id) {
        investimentos.deleteById(id);
    }

    // ------------------------------------------------------------------ Aba 6: Retencao

    @GetMapping("/retencao")
    public Map<String, Object> retencao() {
        return indicadores.retencao();
    }

    // ------------------------------------------------------------------ Aba 7: Relatorios e IA

    @GetMapping("/indicadores")
    public Map<String, Object> indicadores(@RequestParam int ano, @RequestParam int mes) {
        return indicadores.tudo(YearMonth.of(ano, mes));
    }

    @GetMapping("/relatorios")
    public List<RelatorioIA> relatorios(@RequestParam int ano, @RequestParam int mes) {
        return ia.listar(ano, mes);
    }

    /** A analise so e gerada quando o nutricionista pede (RN-032). */
    @PostMapping("/relatorios")
    @ResponseStatus(HttpStatus.CREATED)
    public RelatorioIA gerarRelatorio(@RequestParam int ano, @RequestParam int mes) {
        return ia.gerar(YearMonth.of(ano, mes));
    }

    // ------------------------------------------------------------------ Aba 8: Configuracoes

    public record DataSimuladaReq(LocalDate data) {}

    @GetMapping("/configuracoes")
    public Map<String, Object> configuracoes() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("demo", relogio.isDemo());
        m.put("hoje", relogio.hoje());
        m.put("dataSimulada", relogio.isDemo() ? configuracao().getDataSimulada() : null);
        m.put("iaConfigurada", ia.configurada());
        return m;
    }

    /** Define (ou limpa, com data nula) a data simulada. So existe no ambiente de demonstracao e nao cria registros (RN-036). */
    @PutMapping("/configuracoes/data-simulada")
    public Map<String, Object> salvarDataSimulada(@RequestBody DataSimuladaReq req) {
        if (!relogio.isDemo())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A data simulada só existe no ambiente de demonstração");
        Configuracao c = configuracao();
        c.setDataSimulada(req.data());
        configuracoes.save(c);
        return configuracoes();
    }
}
