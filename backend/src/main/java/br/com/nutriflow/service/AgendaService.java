package br.com.nutriflow.service;

import static br.com.nutriflow.model.Enums.StatusAgendamento.CONFIRMADO;
import static br.com.nutriflow.model.Enums.StatusAgendamento.PENDENTE;

import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.Agendamento;
import br.com.nutriflow.model.Bloqueio;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import br.com.nutriflow.model.PeriodoAtendimento;
import br.com.nutriflow.repository.Repositorios.AgendamentoRepository;
import br.com.nutriflow.repository.Repositorios.BloqueioRepository;
import br.com.nutriflow.repository.Repositorios.PeriodoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Disponibilidade semanal, bloqueios e geracao dos horarios de 60 minutos (RF-007 a RF-010). */
@Service
public class AgendaService {
    /** Status que ocupam um horario: pendentes reservam, confirmados ocupam (RN-007). */
    public static final List<StatusAgendamento> ATIVOS = List.of(PENDENTE, CONFIRMADO);
    public static final int DURACAO_MINUTOS = 60;

    private final PeriodoRepository periodos;
    private final BloqueioRepository bloqueios;
    private final AgendamentoRepository agendamentos;
    private final Relogio relogio;

    public AgendaService(PeriodoRepository periodos, BloqueioRepository bloqueios,
                         AgendamentoRepository agendamentos, Relogio relogio) {
        this.periodos = periodos;
        this.bloqueios = bloqueios;
        this.agendamentos = agendamentos;
        this.relogio = relogio;
    }

    /** Horarios gerados pela disponibilidade semanal para um dia, sem considerar bloqueios. */
    public static List<LocalTime> horariosGerados(LocalDate dia, List<PeriodoAtendimento> periodos) {
        TreeSet<LocalTime> horarios = new TreeSet<>();
        for (PeriodoAtendimento p : periodos) {
            if (p.getDiaSemana() != dia.getDayOfWeek()) continue;
            LocalTime hora = p.getInicio();
            // So entra o horario que cabe inteiro dentro do periodo.
            while (!hora.plusMinutes(DURACAO_MINUTOS).isAfter(p.getFim()) && hora.plusMinutes(DURACAO_MINUTOS).isAfter(hora)) {
                horarios.add(hora);
                hora = hora.plusMinutes(DURACAO_MINUTOS);
            }
        }
        return new ArrayList<>(horarios);
    }

    public static boolean bloqueado(LocalDate dia, LocalTime hora, List<Bloqueio> bloqueios) {
        return bloqueios.stream().anyMatch(b -> b.atinge(dia, hora));
    }

    /** Horarios de atendimento de um dia: gerados pela disponibilidade e fora dos bloqueios (RN-006). */
    public static List<LocalTime> horariosDeAtendimento(LocalDate dia, List<PeriodoAtendimento> periodos, List<Bloqueio> bloqueios) {
        return horariosGerados(dia, periodos).stream().filter(h -> !bloqueado(dia, h, bloqueios)).toList();
    }

    /** Horarios que o paciente pode solicitar: de atendimento, livres e no futuro. */
    public List<LocalTime> horariosLivres(LocalDate dia) {
        LocalDate hoje = relogio.hoje();
        if (dia.isBefore(hoje)) return List.of();
        Set<LocalTime> ocupados = agendamentos.findByDataAndStatusIn(dia, ATIVOS).stream()
                .map(Agendamento::getHora).collect(Collectors.toSet());
        LocalTime agora = LocalTime.now();
        return horariosDeAtendimento(dia, periodos.findAll(), bloqueios.findAll()).stream()
                .filter(h -> !ocupados.contains(h))
                .filter(h -> dia.isAfter(hoje) || h.isAfter(agora))
                .toList();
    }

    /** Visao do nutricionista: cada horario do dia com seu status (Disponivel, Pendente, Confirmado, Bloqueado). */
    public List<Map<String, Object>> agendaDoDia(LocalDate dia) {
        List<Bloqueio> todosBloqueios = bloqueios.findAll();
        Map<LocalTime, Agendamento> ocupados = agendamentos.findByDataAndStatusIn(dia, ATIVOS).stream()
                .collect(Collectors.toMap(Agendamento::getHora, a -> a, (a, b) -> a));
        TreeSet<LocalTime> horas = new TreeSet<>(horariosGerados(dia, periodos.findAll()));
        horas.addAll(ocupados.keySet()); // consultas marcadas antes de uma mudanca de disponibilidade continuam visiveis

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (LocalTime hora : horas) {
            Map<String, Object> item = new LinkedHashMap<>();
            Agendamento a = ocupados.get(hora);
            item.put("hora", hora);
            if (a != null) {
                item.put("status", a.getStatus().name());
                item.put("agendamentoId", a.getId());
                item.put("paciente", a.getPaciente().getNome());
                item.put("origem", a.getOrigem().name());
            } else {
                item.put("status", bloqueado(dia, hora, todosBloqueios) ? "BLOQUEADO" : "DISPONIVEL");
            }
            resultado.add(item);
        }
        return resultado;
    }

    public List<PeriodoAtendimento> listarDisponibilidade() {
        return periodos.findAll().stream()
                .sorted(Comparator.comparing(PeriodoAtendimento::getDiaSemana).thenComparing(PeriodoAtendimento::getInicio))
                .toList();
    }

    /** Substitui toda a disponibilidade semanal. Agendamentos ja existentes nao sao alterados. */
    @Transactional
    public List<PeriodoAtendimento> salvarDisponibilidade(List<PeriodoAtendimento> novos) {
        for (PeriodoAtendimento p : novos) {
            if (p.getDiaSemana() == null || p.getInicio() == null || p.getFim() == null)
                throw erro("Preencha dia, início e término de todos os períodos");
            if (!p.getFim().isAfter(p.getInicio()))
                throw erro("O término deve ser depois do início");
            if (p.getInicio().plusMinutes(DURACAO_MINUTOS).isAfter(p.getFim()))
                throw erro("Cada período precisa ter pelo menos 60 minutos");
        }
        for (int i = 0; i < novos.size(); i++) {
            for (int j = i + 1; j < novos.size(); j++) {
                PeriodoAtendimento a = novos.get(i), b = novos.get(j);
                if (a.getDiaSemana() == b.getDiaSemana() && a.getInicio().isBefore(b.getFim()) && b.getInicio().isBefore(a.getFim()))
                    throw erro("Há períodos sobrepostos no mesmo dia");
            }
        }
        periodos.deleteAllInBatch();
        periodos.saveAll(novos.stream().map(p -> new PeriodoAtendimento(p.getDiaSemana(), p.getInicio(), p.getFim())).toList());
        return listarDisponibilidade();
    }

    public List<Bloqueio> listarBloqueios() {
        return bloqueios.findAll().stream().sorted(Comparator.comparing(Bloqueio::getData).reversed()).toList();
    }

    /** Cria um bloqueio. E recusado se atingir um horario ja pendente ou confirmado. */
    public Bloqueio criarBloqueio(Bloqueio b) {
        if ((b.getInicio() == null) != (b.getFim() == null))
            throw erro("Informe o horário inicial e o final, ou deixe os dois vazios para bloquear o dia inteiro");
        if (b.getInicio() != null && !b.getFim().isAfter(b.getInicio()))
            throw erro("O horário final deve ser depois do inicial");

        List<Agendamento> candidatos = b.isRecorrente()
                ? agendamentos.findByDataGreaterThanEqualAndStatusIn(b.getData(), ATIVOS)
                : agendamentos.findByDataAndStatusIn(b.getData(), ATIVOS);
        Optional<Agendamento> conflito = candidatos.stream().filter(a -> b.atinge(a.getData(), a.getHora())).findFirst();
        if (conflito.isPresent()) {
            Agendamento a = conflito.get();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bloqueio recusado: já existe agendamento "
                    + a.getStatus().name().toLowerCase() + " de " + a.getPaciente().getNome()
                    + " em " + a.getData() + " às " + a.getHora());
        }
        return bloqueios.save(b);
    }

    public void removerBloqueio(Long id) {
        bloqueios.deleteById(id);
    }

    private static ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
