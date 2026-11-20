package br.com.nutriflow.controller;

import br.com.nutriflow.model.Enums.Origem;
import br.com.nutriflow.repository.Repositorios.AgendamentoRepository;
import br.com.nutriflow.service.AgendaService;
import br.com.nutriflow.service.AgendamentoService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Ambiente do paciente (RF-005, RF-010, RF-011). O id do paciente vem sempre do token de acesso,
 * nunca da URL ou do corpo da requisicao, entao um paciente nao consegue ver dados de outro (RNF-008).
 */
@RestController
@RequestMapping("/api/paciente")
public class PacienteController {
    private final AgendaService agenda;
    private final AgendamentoService servico;
    private final AgendamentoRepository agendamentos;

    public PacienteController(AgendaService agenda, AgendamentoService servico, AgendamentoRepository agendamentos) {
        this.agenda = agenda;
        this.servico = servico;
        this.agendamentos = agendamentos;
    }

    public record SolicitacaoReq(LocalDate data, LocalTime hora, Origem origem) {}

    @GetMapping("/horarios")
    public List<LocalTime> horarios(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.horariosLivres(data);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> solicitar(@AuthenticationPrincipal Long pacienteId, @RequestBody SolicitacaoReq req) {
        return AgendamentoService.paraJson(servico.solicitar(pacienteId, req.data(), req.hora(), req.origem()), false);
    }

    @GetMapping("/agendamentos")
    public List<Map<String, Object>> meusAgendamentos(@AuthenticationPrincipal Long pacienteId) {
        return agendamentos.findByPacienteIdOrderByDataDescHoraDesc(pacienteId).stream()
                .map(a -> AgendamentoService.paraJson(a, false)).toList();
    }
}
