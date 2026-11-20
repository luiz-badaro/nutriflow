import { useEffect, useState } from 'react'
import { api } from '../../api.js'
import { Aviso, Estado, Selo, Subabas, Tabela, useCarregar } from '../../componentes.jsx'
import { DIAS, dataBR, horaCurta, moeda, rotulo } from '../../util.js'

const SUBABAS = ['Calendário', 'Solicitações', 'Disponibilidade', 'Bloqueios']

/** Aba 2 - Agenda. */
export default function Agenda({ hoje }) {
  const [sub, setSub] = useState(SUBABAS[1])
  return (
    <>
      <Subabas itens={SUBABAS} atual={sub} onMudar={setSub} />
      {sub === 'Calendário' && <Calendario hoje={hoje} />}
      {sub === 'Solicitações' && <Solicitacoes />}
      {sub === 'Disponibilidade' && <Disponibilidade />}
      {sub === 'Bloqueios' && <Bloqueios hoje={hoje} />}
    </>
  )
}

function somarDias(iso, dias) {
  const d = new Date(iso + 'T12:00:00')
  d.setDate(d.getDate() + dias)
  return d.toISOString().slice(0, 10)
}

/** 2.1 - Horarios de um dia com o status de cada um. */
function Calendario({ hoje }) {
  const [data, setData] = useState(hoje)
  const carga = useCarregar(() => api('/admin/agenda/dia?data=' + data), [data])
  return (
    <section className="painel">
      <div className="linha-acoes">
        <button onClick={() => setData(somarDias(data, -1))} aria-label="Dia anterior">◀</button>
        <input type="date" value={data} onChange={(e) => e.target.value && setData(e.target.value)} aria-label="Data" />
        <button onClick={() => setData(somarDias(data, 1))} aria-label="Próximo dia">▶</button>
        <button onClick={() => setData(hoje)}>Hoje</button>
      </div>
      <Estado carga={carga}>
        {(horarios) => (
          <Tabela
            vazio="Não há atendimento neste dia. Configure a disponibilidade semanal."
            linhas={horarios}
            colunas={[
              { titulo: 'Horário', celula: (h) => horaCurta(h.hora) },
              { titulo: 'Status', celula: (h) => <Selo status={h.status} /> },
              { titulo: 'Paciente', celula: (h) => h.paciente ?? '—' },
              { titulo: 'Origem', celula: (h) => (h.origem ? rotulo(h.origem) : '—') },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}

/** 2.2 - Solicitacoes: confirmar ou negar as pendentes. A decisao e final. */
function Solicitacoes() {
  const [status, setStatus] = useState('PENDENTE')
  const [erro, setErro] = useState(null)
  const [mensagem, setMensagem] = useState(null)
  const [ocupado, setOcupado] = useState(null)
  const carga = useCarregar(() => api('/admin/agendamentos' + (status ? '?status=' + status : '')), [status])

  async function decidir(agendamento, acao) {
    const confirmando = acao === 'confirmar'
    const texto = confirmando
      ? `Confirmar a consulta de ${agendamento.paciente}? Ela será considerada realizada e paga, e uma receita de ${moeda(agendamento.valor)} será gerada. Não é possível desfazer.`
      : `Negar a solicitação de ${agendamento.paciente}? O horário será liberado. Não é possível desfazer.`
    if (!window.confirm(texto)) return
    setErro(null)
    setMensagem(null)
    setOcupado(agendamento.id)
    try {
      await api(`/admin/agendamentos/${agendamento.id}/${acao}`, { method: 'POST' })
      setMensagem(confirmando ? 'Consulta confirmada e receita registrada.' : 'Solicitação negada e horário liberado.')
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setOcupado(null)
      carga.recarregar()
    }
  }

  return (
    <section className="painel">
      <div className="linha-acoes">
        <label>Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="PENDENTE">Pendentes</option>
            <option value="CONFIRMADO">Confirmadas</option>
            <option value="NEGADO">Negadas</option>
            <option value="">Todas</option>
          </select>
        </label>
      </div>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{mensagem}</Aviso>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio={status === 'PENDENTE' ? 'Nenhuma solicitação aguardando decisão.' : 'Nenhuma solicitação com este status.'}
            linhas={lista}
            colunas={[
              { titulo: 'Paciente', celula: (a) => a.paciente },
              { titulo: 'Data', celula: (a) => dataBR(a.data) },
              { titulo: 'Horário', celula: (a) => horaCurta(a.hora) },
              { titulo: 'Origem', celula: (a) => rotulo(a.origem) },
              { titulo: 'Valor', numero: true, celula: (a) => moeda(a.valor) },
              { titulo: 'Status', celula: (a) => <Selo status={a.status} /> },
              {
                titulo: 'Decisão',
                celula: (a) =>
                  a.status === 'PENDENTE' ? (
                    <span className="acoes">
                      <button className="primario pequeno" disabled={ocupado === a.id} onClick={() => decidir(a, 'confirmar')}>Confirmar</button>
                      <button className="perigo pequeno" disabled={ocupado === a.id} onClick={() => decidir(a, 'negar')}>Negar</button>
                    </span>
                  ) : (
                    <span className="suave">Final</span>
                  ),
              },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}

/** 2.3 - Disponibilidade semanal, com varios periodos por dia. */
function Disponibilidade() {
  const [periodos, setPeriodos] = useState(null)
  const [erro, setErro] = useState(null)
  const [mensagem, setMensagem] = useState(null)

  useEffect(() => {
    api('/admin/disponibilidade')
      .then((lista) => setPeriodos(lista.map((p) => ({ diaSemana: p.diaSemana, inicio: horaCurta(p.inicio), fim: horaCurta(p.fim) }))))
      .catch((falha) => setErro(falha.message))
  }, [])

  const alterar = (i, campo, valor) => setPeriodos(periodos.map((p, j) => (j === i ? { ...p, [campo]: valor } : p)))

  async function salvar() {
    setErro(null)
    setMensagem(null)
    try {
      await api('/admin/disponibilidade', { method: 'PUT', body: periodos })
      setMensagem('Disponibilidade salva. Os horários de 60 minutos foram gerados a partir dela.')
    } catch (falha) {
      setErro(falha.message)
    }
  }

  if (!periodos) return <Aviso tipo="erro">{erro}</Aviso>
  return (
    <section className="painel">
      <p className="suave">Cada período gera horários de 60 minutos. Exemplo: 08:00 às 12:00 gera 08:00, 09:00, 10:00 e 11:00. Agendamentos já feitos não são alterados.</p>
      {!periodos.length && <p className="suave vazio">Nenhum período cadastrado: os pacientes não verão horários.</p>}
      {periodos.map((p, i) => (
        <div className="linha-form" key={i}>
          <label>Dia da semana
            <select value={p.diaSemana} onChange={(e) => alterar(i, 'diaSemana', e.target.value)}>
              {DIAS.map((d) => <option key={d} value={d}>{rotulo(d)}</option>)}
            </select>
          </label>
          <label>Início<input type="time" value={p.inicio} onChange={(e) => alterar(i, 'inicio', e.target.value)} /></label>
          <label>Término<input type="time" value={p.fim} onChange={(e) => alterar(i, 'fim', e.target.value)} /></label>
          <button className="perigo pequeno" onClick={() => setPeriodos(periodos.filter((_, j) => j !== i))}>Remover</button>
        </div>
      ))}
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{mensagem}</Aviso>
      <div className="linha-acoes">
        <button onClick={() => setPeriodos([...periodos, { diaSemana: 'MONDAY', inicio: '08:00', fim: '12:00' }])}>Adicionar período</button>
        <button className="primario" onClick={salvar}>Salvar disponibilidade</button>
      </div>
    </section>
  )
}

/** 2.4 - Bloqueios de dia inteiro ou de um periodo, com opcao de repetir toda semana. */
function Bloqueios({ hoje }) {
  const vazio = { data: hoje, inicio: '', fim: '', recorrente: false, motivo: '' }
  const [form, setForm] = useState(vazio)
  const [erro, setErro] = useState(null)
  const carga = useCarregar(() => api('/admin/bloqueios'))

  async function salvar(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api('/admin/bloqueios', { method: 'POST', body: { ...form, inicio: form.inicio || null, fim: form.fim || null } })
      setForm(vazio)
      carga.recarregar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  async function remover(id) {
    if (!window.confirm('Remover este bloqueio? Os horários voltam a ficar disponíveis.')) return
    try {
      await api('/admin/bloqueios/' + id, { method: 'DELETE' })
      carga.recarregar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  return (
    <section className="painel">
      <form className="linha-form" onSubmit={salvar}>
        <label>Data<input type="date" required value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></label>
        <label>Horário inicial<input type="time" value={form.inicio} onChange={(e) => setForm({ ...form, inicio: e.target.value })} /></label>
        <label>Horário final<input type="time" value={form.fim} onChange={(e) => setForm({ ...form, fim: e.target.value })} /></label>
        <label>Motivo (opcional)<input value={form.motivo} onChange={(e) => setForm({ ...form, motivo: e.target.value })} /></label>
        <label className="caixa">
          <input type="checkbox" checked={form.recorrente} onChange={(e) => setForm({ ...form, recorrente: e.target.checked })} />
          Repetir toda semana
        </label>
        <button className="primario">Salvar bloqueio</button>
      </form>
      <p className="suave">Deixe os horários vazios para bloquear o dia inteiro. O bloqueio é recusado se atingir um horário já pendente ou confirmado.</p>
      <Aviso tipo="erro">{erro}</Aviso>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Nenhum bloqueio cadastrado."
            linhas={lista}
            colunas={[
              { titulo: 'Data', celula: (b) => dataBR(b.data) },
              { titulo: 'Período', celula: (b) => (b.inicio ? `${horaCurta(b.inicio)} às ${horaCurta(b.fim)}` : 'Dia inteiro') },
              { titulo: 'Repetição', celula: (b) => (b.recorrente ? 'Toda semana a partir da data' : 'Só nesta data') },
              { titulo: 'Motivo', celula: (b) => b.motivo || '—' },
              { titulo: '', celula: (b) => <button className="perigo pequeno" onClick={() => remover(b.id)}>Remover</button> },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}
