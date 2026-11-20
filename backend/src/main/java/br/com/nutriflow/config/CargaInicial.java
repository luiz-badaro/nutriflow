package br.com.nutriflow.config;

import br.com.nutriflow.model.*;
import br.com.nutriflow.model.Enums.*;
import br.com.nutriflow.repository.Repositorios.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Executa ao iniciar o servidor:
 * 1) garante a configuracao do consultorio e a conta do nutricionista (que nao pode nascer do cadastro publico);
 * 2) no ambiente de demonstracao, e so com o banco vazio, cria dados ficticios dos tres meses anteriores (RF-039).
 */
@Component
public class CargaInicial implements CommandLineRunner {
    public static final String SENHA_PACIENTES_DEMO = "paciente123";

    private final UsuarioRepository usuarios;
    private final ConfiguracaoRepository configuracoes;
    private final PeriodoRepository periodos;
    private final AgendamentoRepository agendamentos;
    private final ReceitaRepository receitas;
    private final DespesaRepository despesas;
    private final InvestimentoRepository investimentos;
    private final PasswordEncoder encoder;
    private final Relogio relogio;
    @Value("${nutriflow.admin.nome}") private String adminNome;
    @Value("${nutriflow.admin.email}") private String adminEmail;
    @Value("${nutriflow.admin.senha}") private String adminSenha;
    @Value("${nutriflow.demo.carga:true}") private boolean cargaDemo;

    public CargaInicial(UsuarioRepository usuarios, ConfiguracaoRepository configuracoes, PeriodoRepository periodos,
                        AgendamentoRepository agendamentos, ReceitaRepository receitas, DespesaRepository despesas,
                        InvestimentoRepository investimentos, PasswordEncoder encoder, Relogio relogio) {
        this.usuarios = usuarios;
        this.configuracoes = configuracoes;
        this.periodos = periodos;
        this.agendamentos = agendamentos;
        this.receitas = receitas;
        this.despesas = despesas;
        this.investimentos = investimentos;
        this.encoder = encoder;
        this.relogio = relogio;
    }

    @Override
    public void run(String... args) {
        if (!configuracoes.existsById(Configuracao.ID)) configuracoes.save(new Configuracao());
        if (!usuarios.existsByPerfil(Perfil.NUTRICIONISTA)) {
            Usuario admin = new Usuario();
            admin.setNome(adminNome);
            admin.setEmail(adminEmail.toLowerCase());
            admin.setSenhaHash(encoder.encode(adminSenha));
            admin.setPerfil(Perfil.NUTRICIONISTA);
            usuarios.save(admin);
        }
        boolean bancoVazio = agendamentos.count() == 0 && !usuarios.existsByPerfil(Perfil.PACIENTE);
        if (relogio.isDemo() && cargaDemo && bancoVazio) criarDadosFicticios();
    }

    private void criarDadosFicticios() {
        Random sorteio = new Random(2026); // semente fixa: a demonstracao comeca sempre com os mesmos dados
        BigDecimal valor = configuracoes.findById(Configuracao.ID).orElseThrow().getValorConsulta();

        // Disponibilidade: segunda a sexta, 08-12 e 14-18.
        for (DayOfWeek dia : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            periodos.save(new PeriodoAtendimento(dia, LocalTime.of(8, 0), LocalTime.of(12, 0)));
            periodos.save(new PeriodoAtendimento(dia, LocalTime.of(14, 0), LocalTime.of(18, 0)));
        }
        List<LocalTime> horarios = List.of(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0));

        String[] nomes = {"Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Rocha", "Elisa Prado", "Fábio Nunes",
                "Gabriela Reis", "Hugo Martins", "Isabela Costa", "João Pereira", "Karina Alves", "Lucas Barros",
                "Mariana Dias", "Nelson Farias", "Olívia Ramos", "Paulo Teixeira"};
        String senha = encoder.encode(SENHA_PACIENTES_DEMO);
        List<Usuario> pacientes = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            Usuario p = new Usuario();
            p.setNome(nomes[i]);
            String semAcento = java.text.Normalizer.normalize(nomes[i], java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            p.setEmail(semAcento.toLowerCase().replace(" ", ".") + "@exemplo.com");
            p.setTelefone("(11) 9" + (1000 + sorteio.nextInt(9000)) + "-" + (1000 + sorteio.nextInt(9000)));
            p.setIdade(18 + sorteio.nextInt(48));
            p.setSexo(i % 3 == 1 ? Sexo.MASCULINO : Sexo.FEMININO);
            p.setObjetivo(Objetivo.values()[sorteio.nextInt(Objetivo.values().length - 1)]);
            p.setSenhaHash(senha);
            p.setPerfil(Perfil.PACIENTE);
            pacientes.add(usuarios.save(p));
        }

        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        double[] procura = {0.30, 0.38, 0.46};           // a agenda vai enchendo mes a mes
        int[] investInstagram = {400, 500, 600};
        int[] investGoogle = {300, 300, 300};
        for (int i = 0; i < 3; i++) {
            YearMonth mes = mesAtual.minusMonths(3 - i);
            for (LocalDate dia = mes.atDay(1); !dia.isAfter(mes.atEndOfMonth()); dia = dia.plusDays(1)) {
                if (dia.getDayOfWeek().getValue() > 5) continue;
                for (LocalTime hora : horarios) {
                    // Sexta-feira e o fim da tarde tem menos procura, para a analise de ocupacao ter o que mostrar.
                    double chance = procura[i] * (dia.getDayOfWeek() == DayOfWeek.FRIDAY ? 0.5 : 1) * (hora.getHour() >= 16 ? 0.6 : 1);
                    if (sorteio.nextDouble() > chance) continue;
                    // Os quatro primeiros pacientes so aparecem no primeiro mes: viram alertas de retencao.
                    Usuario paciente = pacientes.get(i == 0 ? sorteio.nextInt(pacientes.size()) : 4 + sorteio.nextInt(pacientes.size() - 4));
                    criarAgendamento(paciente, dia, hora, sortearOrigem(sorteio), valor,
                            sorteio.nextDouble() < 0.9 ? StatusAgendamento.CONFIRMADO : StatusAgendamento.NEGADO);
                }
            }
            despesa("Aluguel da sala", CategoriaDespesa.ALUGUEL, 1200, mes.atDay(5));
            despesa("Sistema e internet", CategoriaDespesa.SOFTWARE, 180, mes.atDay(10));
            despesa("Materiais de consultório", CategoriaDespesa.MATERIAIS, 220 + 40 * i, mes.atDay(15));
            despesa("Impostos", CategoriaDespesa.IMPOSTOS, 350 + 60 * i, mes.atDay(20));
            investimento(Origem.INSTAGRAM, mes, investInstagram[i]);
            investimento(Origem.GOOGLE, mes, investGoogle[i]);
        }
    }

    private Origem sortearOrigem(Random sorteio) {
        int n = sorteio.nextInt(100);
        if (n < 30) return Origem.INSTAGRAM;
        if (n < 48) return Origem.GOOGLE;
        if (n < 62) return Origem.INDICACAO;
        if (n < 95) return Origem.RETORNO;
        return Origem.OUTRO;
    }

    private void criarAgendamento(Usuario paciente, LocalDate dia, LocalTime hora, Origem origem, BigDecimal valor, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setPaciente(paciente);
        a.setData(dia);
        a.setHora(hora);
        a.setOrigem(origem);
        a.setValor(valor);
        a.setStatus(status);
        a.setCriadoEm(dia.minusDays(3).atTime(12, 0));
        a.setDecididoEm(dia.minusDays(2).atTime(12, 0));
        a.setSlotAtivo(status == StatusAgendamento.NEGADO ? null : dia + "T" + hora);
        agendamentos.save(a);
        if (status == StatusAgendamento.CONFIRMADO) receitas.save(new Receita(a));
    }

    private void despesa(String descricao, CategoriaDespesa categoria, int valor, LocalDate data) {
        Despesa d = new Despesa();
        d.setDescricao(descricao);
        d.setCategoria(categoria);
        d.setValor(BigDecimal.valueOf(valor));
        d.setData(data);
        despesas.save(d);
    }

    private void investimento(Origem canal, YearMonth mes, int valor) {
        InvestimentoMarketing inv = new InvestimentoMarketing();
        inv.setCanal(canal);
        inv.setAno(mes.getYear());
        inv.setMes(mes.getMonthValue());
        inv.setValor(BigDecimal.valueOf(valor));
        investimentos.save(inv);
    }
}
