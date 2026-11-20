package br.com.nutriflow.repository;

import br.com.nutriflow.model.*;
import br.com.nutriflow.model.Enums.Perfil;
import br.com.nutriflow.model.Enums.StatusAgendamento;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorios Spring Data JPA, reunidos em um unico arquivo por serem todos muito pequenos. */
public final class Repositorios {
    private Repositorios() {}

    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
        Optional<Usuario> findByEmailIgnoreCase(String email);
        Optional<Usuario> findByTokenRecuperacao(String token);
        List<Usuario> findByPerfilOrderByNome(Perfil perfil);
        boolean existsByPerfil(Perfil perfil);
    }

    public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
        List<Agendamento> findByDataBetween(LocalDate inicio, LocalDate fim);
        List<Agendamento> findByDataAndStatusIn(LocalDate data, Collection<StatusAgendamento> status);
        List<Agendamento> findByDataGreaterThanEqualAndStatusIn(LocalDate data, Collection<StatusAgendamento> status);
        List<Agendamento> findByPacienteIdOrderByDataDescHoraDesc(Long pacienteId);
        List<Agendamento> findByStatus(StatusAgendamento status);
        List<Agendamento> findAllByOrderByDataDescHoraDesc();
    }

    public interface ReceitaRepository extends JpaRepository<Receita, Long> {
        List<Receita> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);
        boolean existsByAgendamentoId(Long agendamentoId);
    }

    public interface DespesaRepository extends JpaRepository<Despesa, Long> {
        List<Despesa> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);
    }

    public interface InvestimentoRepository extends JpaRepository<InvestimentoMarketing, Long> {
        List<InvestimentoMarketing> findByAnoAndMes(int ano, int mes);
    }

    public interface PeriodoRepository extends JpaRepository<PeriodoAtendimento, Long> {}

    public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {}

    public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {}

    public interface RelatorioRepository extends JpaRepository<RelatorioIA, Long> {
        List<RelatorioIA> findByAnoAndMesOrderByGeradoEmDesc(int ano, int mes);
    }
}
