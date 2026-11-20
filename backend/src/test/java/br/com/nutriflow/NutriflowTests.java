package br.com.nutriflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.nutriflow.config.JwtService;
import br.com.nutriflow.model.*;
import br.com.nutriflow.model.Enums.*;
import br.com.nutriflow.repository.Repositorios.*;
import br.com.nutriflow.service.AgendaService;
import br.com.nutriflow.service.AgendamentoService;
import br.com.nutriflow.service.IaService;
import br.com.nutriflow.service.IndicadoresService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

/** Testes de sucesso e de erro dos fluxos principais (RNF-018). Usa um banco H2 em memoria, sem dados ficticios. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "nutriflow.demo=true",
        "nutriflow.demo.carga=false",
        "nutriflow.gemini.api-key="
})
class NutriflowTests {
    private static final YearMonth MARCO = YearMonth.of(2026, 3);
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 2);     // segunda-feira
    private static final LocalDate TERCA = LocalDate.of(2026, 3, 3);

    @Autowired WebApplicationContext contexto;
    @Autowired AgendaService agenda;
    @Autowired AgendamentoService servico;
    @Autowired IndicadoresService indicadores;
    @Autowired IaService ia;
    @Autowired JwtService jwt;
    @Autowired UsuarioRepository usuarios;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired ReceitaRepository receitas;
    @Autowired DespesaRepository despesas;
    @Autowired InvestimentoRepository investimentos;
    @Autowired PeriodoRepository periodos;
    @Autowired BloqueioRepository bloqueios;
    @Autowired ConfiguracaoRepository configuracoes;

    Usuario joao, maria, pedro;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        receitas.deleteAll();
        agendamentos.deleteAll();
        despesas.deleteAll();
        investimentos.deleteAll();
        bloqueios.deleteAll();
        periodos.deleteAll();
        usuarios.deleteAll(usuarios.findByPerfilOrderByNome(Perfil.PACIENTE));

        Configuracao c = configuracoes.findById(Configuracao.ID).orElseGet(Configuracao::new);
        c.setValorConsulta(new BigDecimal("150.00"));
        c.setDataSimulada(HOJE);
        configuracoes.save(c);
        for (DayOfWeek dia : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            periodos.save(new PeriodoAtendimento(dia, LocalTime.of(8, 0), LocalTime.of(12, 0)));
            periodos.save(new PeriodoAtendimento(dia, LocalTime.of(14, 0), LocalTime.of(16, 30)));
        }
        joao = paciente("Joao");
        maria = paciente("Maria");
        pedro = paciente("Pedro");
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Usuario paciente(String nome) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@teste.com");
        u.setSenhaHash("x");
        u.setPerfil(Perfil.PACIENTE);
        u.setIdade(30);
        u.setSexo(Sexo.OUTRO);
        u.setObjetivo(Objetivo.SAUDE);
        return usuarios.save(u);
    }

    private Agendamento confirmada(Usuario paciente, int hora, Origem origem) {
        Agendamento a = servico.solicitar(paciente.getId(), TERCA, LocalTime.of(hora, 0), origem);
        return servico.decidir(a.getId(), true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> canal(Map<String, Object> marketing, String nome) {
        return ((List<Map<String, Object>>) marketing.get("canais")).stream()
                .filter(c -> c.get("canal").equals(nome)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- Agenda

    @Test
    void geraHorariosDe60MinutosSoOndeCabemInteiros() {
        // 08-12 gera 4 horarios; 14-16:30 gera 2 (o de 16:00 nao cabe inteiro).
        assertThat(agenda.horariosLivres(TERCA)).containsExactly(
                LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0));
        assertThat(agenda.horariosLivres(LocalDate.of(2026, 3, 7))).isEmpty();  // sabado
        assertThat(agenda.horariosLivres(LocalDate.of(2026, 2, 27))).isEmpty(); // data passada
    }

    @Test
    void bloqueioRecorrenteTiraOHorarioTodaSemana() {
        Bloqueio b = new Bloqueio();
        b.setData(TERCA);
        b.setInicio(LocalTime.of(8, 0));
        b.setFim(LocalTime.of(10, 0));
        b.setRecorrente(true);
        agenda.criarBloqueio(b);
        assertThat(agenda.horariosLivres(TERCA)).doesNotContain(LocalTime.of(8, 0), LocalTime.of(9, 0)).contains(LocalTime.of(10, 0));
        assertThat(agenda.horariosLivres(TERCA.plusWeeks(1))).doesNotContain(LocalTime.of(8, 0));
        assertThat(agenda.horariosLivres(TERCA.plusDays(1))).contains(LocalTime.of(8, 0));
    }

    @Test
    void bloqueioSobreHorarioOcupadoERecusado() {
        servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.GOOGLE);
        Bloqueio diaInteiro = new Bloqueio();
        diaInteiro.setData(TERCA);
        assertThatThrownBy(() -> agenda.criarBloqueio(diaInteiro)).isInstanceOf(ResponseStatusException.class);
        assertThat(bloqueios.count()).isZero();
    }

    // ---------------------------------------------------------------- Agendamentos

    @Test
    void pendenteReservaOHorarioENaoGeraReceita() {
        servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.INSTAGRAM);
        assertThat(agenda.horariosLivres(TERCA)).doesNotContain(LocalTime.of(9, 0));
        assertThat(receitas.count()).isZero();
        assertThatThrownBy(() -> servico.solicitar(maria.getId(), TERCA, LocalTime.of(9, 0), Origem.GOOGLE))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(agendamentos.count()).isEqualTo(1);
    }

    @Test
    void oBancoImpedeDoisAgendamentosAtivosNoMesmoHorario() {
        Agendamento a = servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.INSTAGRAM);
        Agendamento copia = new Agendamento();
        copia.setPaciente(maria);
        copia.setData(a.getData());
        copia.setHora(a.getHora());
        copia.setOrigem(Origem.GOOGLE);
        copia.setStatus(StatusAgendamento.PENDENTE);
        copia.setValor(a.getValor());
        copia.setSlotAtivo(a.getSlotAtivo());
        assertThatThrownBy(() -> agendamentos.saveAndFlush(copia)).isInstanceOf(Exception.class);
    }

    @Test
    void negarLiberaOHorarioSemReceita() {
        Agendamento a = servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.INSTAGRAM);
        servico.decidir(a.getId(), false);
        assertThat(agenda.horariosLivres(TERCA)).contains(LocalTime.of(9, 0));
        assertThat(receitas.count()).isZero();
        assertThat(servico.solicitar(maria.getId(), TERCA, LocalTime.of(9, 0), Origem.GOOGLE).getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
    }

    @Test
    void confirmarGeraUmaUnicaReceitaEADecisaoEFinal() {
        Agendamento a = servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.INSTAGRAM);
        servico.decidir(a.getId(), true);
        assertThatThrownBy(() -> servico.decidir(a.getId(), true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servico.decidir(a.getId(), false)).isInstanceOf(ResponseStatusException.class);
        assertThat(receitas.count()).isEqualTo(1);
        assertThat(agendamentos.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void mudarOValorDaConsultaNaoAlteraAgendamentosAntigos() {
        Agendamento antigo = confirmada(joao, 9, Origem.RETORNO);
        Configuracao c = configuracoes.findById(Configuracao.ID).orElseThrow();
        c.setValorConsulta(new BigDecimal("180.00"));
        configuracoes.save(c);
        Agendamento novo = confirmada(joao, 10, Origem.RETORNO);
        assertThat(agendamentos.findById(antigo.getId()).orElseThrow().getValor()).isEqualByComparingTo("150.00");
        assertThat(novo.getValor()).isEqualByComparingTo("180.00");
        assertThat((BigDecimal) indicadores.financeiro(MARCO).get("receita")).isEqualByComparingTo("330.00");
    }

    // ---------------------------------------------------------------- Indicadores

    /** Exemplo da especificacao de telas (p.21-22): cinco consultas confirmadas de R$ 150. */
    @Test
    void exemploDasCincoConsultasDoDocumento() {
        confirmada(joao, 8, Origem.INSTAGRAM);
        confirmada(joao, 9, Origem.RETORNO);
        confirmada(maria, 10, Origem.INSTAGRAM);
        confirmada(joao, 11, Origem.GOOGLE);
        confirmada(pedro, 14, Origem.RETORNO);
        servico.solicitar(pedro.getId(), TERCA, LocalTime.of(15, 0), Origem.INSTAGRAM); // pendente: nao conta

        InvestimentoMarketing inv = new InvestimentoMarketing();
        inv.setCanal(Origem.INSTAGRAM);
        inv.setAno(2026);
        inv.setMes(3);
        inv.setValor(new BigDecimal("200.00"));
        investimentos.save(inv);
        Despesa d = new Despesa();
        d.setDescricao("Aluguel");
        d.setCategoria(CategoriaDespesa.ALUGUEL);
        d.setValor(new BigDecimal("400.00"));
        d.setData(TERCA);
        despesas.save(d);

        Map<String, Object> fin = indicadores.financeiro(MARCO);
        assertThat((BigDecimal) fin.get("receita")).isEqualByComparingTo("750.00");
        assertThat((BigDecimal) fin.get("despesas")).isEqualByComparingTo("400.00");
        assertThat((BigDecimal) fin.get("saldo")).isEqualByComparingTo("350.00");
        assertThat(fin.get("anterior")).isNull(); // fevereiro nao tem dados: nada a comparar

        Map<String, Object> mkt = indicadores.marketing(MARCO);
        Map<String, Object> instagram = canal(mkt, "INSTAGRAM");
        assertThat(instagram.get("conversoes")).isEqualTo(2);
        assertThat((BigDecimal) instagram.get("receitaAtribuida")).isEqualByComparingTo("300.00");
        assertThat((BigDecimal) instagram.get("cpa")).isEqualByComparingTo("100.00");
        assertThat((BigDecimal) instagram.get("roi")).isEqualByComparingTo("50.00");

        Map<String, Object> google = canal(mkt, "GOOGLE");
        assertThat(google.get("conversoes")).isEqualTo(1);
        assertThat((BigDecimal) google.get("receitaAtribuida")).isEqualByComparingTo("150.00");
        assertThat(google.get("roi")).isNull(); // investimento zero: nao calculavel

        Map<String, Object> indicacao = canal(mkt, "INDICACAO");
        assertThat(indicacao.get("conversoes")).isEqualTo(0);
        assertThat(indicacao.get("cpa")).isNull(); // sem conversoes: nao calculavel

        assertThat(mkt.get("consultasConfirmadas")).isEqualTo(5);
        assertThat(mkt.get("consultasRetorno")).isEqualTo(2);
        Map<?, ?> totais = (Map<?, ?>) mkt.get("totais");
        assertThat(totais.get("conversoes")).isEqualTo(3);
        assertThat((BigDecimal) totais.get("receitaAtribuida")).isEqualByComparingTo("450.00");
    }

    @Test
    void formulasDeCpaERoi() {
        assertThat(IndicadoresService.cpa(new BigDecimal("500"), 4)).isEqualByComparingTo("125.00");
        assertThat(IndicadoresService.cpa(new BigDecimal("500"), 0)).isNull();
        assertThat(IndicadoresService.roi(new BigDecimal("900"), new BigDecimal("300"))).isEqualByComparingTo("200.00");
        assertThat(IndicadoresService.roi(new BigDecimal("100"), new BigDecimal("400"))).isEqualByComparingTo("-75.00");
        assertThat(IndicadoresService.roi(new BigDecimal("100"), BigDecimal.ZERO)).isNull();
    }

    @Test
    void ocupacaoContaPendentesEConfirmadosMasNaoNegados() {
        confirmada(joao, 8, Origem.RETORNO);
        servico.solicitar(maria.getId(), TERCA, LocalTime.of(9, 0), Origem.GOOGLE);
        servico.decidir(servico.solicitar(pedro.getId(), TERCA, LocalTime.of(10, 0), Origem.GOOGLE).getId(), false);
        Map<String, Object> ag = indicadores.agenda(MARCO);
        // Marco de 2026 tem 22 dias uteis x 6 horarios = 132.
        assertThat(ag.get("horariosTotais")).isEqualTo(132L);
        assertThat(ag.get("horariosOcupados")).isEqualTo(2);
        assertThat(ag.get("horariosDisponiveis")).isEqualTo(130L);
    }

    @Test
    void alertaDeRetencaoSoDepoisDe45DiasEMudarADataNaoCriaRegistros() {
        confirmada(joao, 8, Origem.INSTAGRAM); // ultima consulta em 03/03
        long registrosAntes = agendamentos.count() + receitas.count();
        assertThat(indicadores.retencao().get("totalAlertas")).isEqualTo(0);

        Configuracao c = configuracoes.findById(Configuracao.ID).orElseThrow();
        c.setDataSimulada(TERCA.plusDays(45));
        configuracoes.save(c);
        assertThat(indicadores.retencao().get("totalAlertas")).isEqualTo(0); // exatamente 45 dias ainda nao alerta

        c.setDataSimulada(TERCA.plusDays(46));
        configuracoes.save(c);
        Map<String, Object> ret = indicadores.retencao();
        assertThat(ret.get("totalAlertas")).isEqualTo(1);
        assertThat(ret.get("acimaDe60Dias")).isEqualTo(0L);
        assertThat(agendamentos.count() + receitas.count()).isEqualTo(registrosAntes);
    }

    // ---------------------------------------------------------------- IA

    @Test
    void dadosEnviadosAIaNaoLevamNomesEExigemHistoricoParaProjecao() {
        confirmada(joao, 8, Origem.INSTAGRAM);
        Map<String, Object> dados = ia.dadosParaAnalise(MARCO);
        assertThat(dados.toString()).doesNotContain("Joao").doesNotContain("joao@teste.com");
        assertThat(dados.get("projecaoPermitida")).isEqualTo(false); // so um mes de historico
    }

    @Test
    void semChaveDaApiAAnaliseFalhaSemGravarNada() {
        assertThatThrownBy(() -> ia.gerar(MARCO)).isInstanceOf(ResponseStatusException.class);
        assertThat(ia.listar(2026, 3)).isEmpty();
    }

    // ---------------------------------------------------------------- Controle de acesso

    @Test
    void acessoPorPerfilEValidadoNoBackend() throws Exception {
        String tokenPaciente = "Bearer " + jwt.gerar(joao);
        String tokenAdmin = "Bearer " + jwt.gerar(usuarios.findByPerfilOrderByNome(Perfil.NUTRICIONISTA).get(0));

        mvc.perform(get("/api/admin/pacientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/pacientes").header("Authorization", tokenPaciente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/pacientes").header("Authorization", tokenAdmin)).andExpect(status().isOk());
        mvc.perform(get("/api/paciente/agendamentos").header("Authorization", tokenAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/paciente/agendamentos").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());

        Agendamento deJoao = servico.solicitar(joao.getId(), TERCA, LocalTime.of(9, 0), Origem.GOOGLE);
        // Paciente nao consegue confirmar a propria solicitacao.
        mvc.perform(post("/api/admin/agendamentos/" + deJoao.getId() + "/confirmar").header("Authorization", tokenPaciente))
                .andExpect(status().isForbidden());
        // Maria so enxerga os proprios agendamentos: a lista dela vem vazia.
        mvc.perform(get("/api/paciente/agendamentos").header("Authorization", "Bearer " + jwt.gerar(maria)))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).isEqualTo("[]"));
    }

    @Test
    void cadastroPublicoCriaSoPacienteERecusaEmailRepetido() throws Exception {
        String corpo = """
                {"nome":"Nova Paciente","email":"nova@teste.com","telefone":"11999990000","idade":28,
                 "sexo":"FEMININO","objetivo":"SAUDE","senha":"segredo1","perfil":"NUTRICIONISTA"}""";
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
        Usuario criada = usuarios.findByEmailIgnoreCase("nova@teste.com").orElseThrow();
        assertThat(criada.getPerfil()).isEqualTo(Perfil.PACIENTE);
        assertThat(criada.getSenhaHash()).isNotEqualTo("segredo1").startsWith("$2");
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nova@teste.com\",\"senha\":\"segredo1\",\"perfil\":\"PACIENTE\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nova@teste.com\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
    }
}
