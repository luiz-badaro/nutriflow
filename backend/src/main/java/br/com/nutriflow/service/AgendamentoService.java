package br.com.nutriflow.service;

import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.Agendamento;
import br.com.nutriflow.model.Configuracao;
import br.com.nutriflow.model.Enums.Origem;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import br.com.nutriflow.model.Receita;
import br.com.nutriflow.model.Usuario;
import br.com.nutriflow.repository.Repositorios.AgendamentoRepository;
import br.com.nutriflow.repository.Repositorios.ConfiguracaoRepository;
import br.com.nutriflow.repository.Repositorios.ReceitaRepository;
import br.com.nutriflow.repository.Repositorios.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Fluxo de solicitacao, confirmacao e negativa de consultas (RF-011 a RF-016). */
@Service
public class AgendamentoService {
    private final AgendamentoRepository agendamentos;
    private final ReceitaRepository receitas;
    private final UsuarioRepository usuarios;
    private final ConfiguracaoRepository configuracoes;
    private final AgendaService agenda;
    private final Relogio relogio;

    public AgendamentoService(AgendamentoRepository agendamentos, ReceitaRepository receitas, UsuarioRepository usuarios,
                              ConfiguracaoRepository configuracoes, AgendaService agenda, Relogio relogio) {
        this.agendamentos = agendamentos;
        this.receitas = receitas;
        this.usuarios = usuarios;
        this.configuracoes = configuracoes;
        this.agenda = agenda;
        this.relogio = relogio;
    }

    /** Cria a solicitacao como Pendente, reservando o horario (RN-007). */
    public Agendamento solicitar(Long pacienteId, LocalDate data, LocalTime hora, Origem origem) {
        if (data == null || hora == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe data e horário");
        if (origem == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe como chegou até este atendimento");
        // O backend confere a disponibilidade de novo, mesmo que a tela ja tenha mostrado o horario como livre.
        if (!agenda.horariosLivres(data).contains(hora))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este horário não está mais disponível");

        Usuario paciente = usuarios.findById(pacienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        Agendamento a = new Agendamento();
        a.setPaciente(paciente);
        a.setData(data);
        a.setHora(hora);
        a.setOrigem(origem);
        a.setStatus(StatusAgendamento.PENDENTE);
        a.setValor(configuracoes.findById(Configuracao.ID).orElseGet(Configuracao::new).getValorConsulta());
        a.setCriadoEm(relogio.agora());
        a.setSlotAtivo(data + "T" + hora);
        try {
            return agendamentos.saveAndFlush(a);
        } catch (DataIntegrityViolationException doisAoMesmoTempo) {
            // O indice unico de slotAtivo barrou uma segunda reserva simultanea do mesmo horario.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este horário acabou de ser reservado por outra pessoa");
        }
    }

    /**
     * Confirma ou nega uma solicitacao pendente. A decisao e final (RN-010).
     * Confirmar = consulta realizada e paga: gera uma unica receita (RN-009, RN-014).
     * Negar libera o horario e nao gera receita (RN-008).
     */
    @Transactional
    public Agendamento decidir(Long id, boolean confirmar) {
        Agendamento a = agendamentos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        if (a.getStatus() != StatusAgendamento.PENDENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitação já foi decidida e não pode ser alterada");

        a.setDecididoEm(relogio.agora());
        if (confirmar) {
            a.setStatus(StatusAgendamento.CONFIRMADO);
            if (!receitas.existsByAgendamentoId(a.getId())) receitas.save(new Receita(a));
        } else {
            a.setStatus(StatusAgendamento.NEGADO);
            a.setSlotAtivo(null);
        }
        return agendamentos.saveAndFlush(a);
    }

    /** Formato enviado ao frontend. Dados do paciente so entram nas telas do nutricionista. */
    public static Map<String, Object> paraJson(Agendamento a, boolean comPaciente) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("data", a.getData());
        m.put("hora", a.getHora());
        m.put("status", a.getStatus().name());
        m.put("origem", a.getOrigem().name());
        if (comPaciente) {
            m.put("valor", a.getValor());
            m.put("pacienteId", a.getPaciente().getId());
            m.put("paciente", a.getPaciente().getNome());
        }
        return m;
    }
}
