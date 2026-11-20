import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, sair, sessao } from '../api.js'
import { Aviso, Estado, Selo, Tabela, useCarregar } from '../componentes.jsx'
import { ORIGENS, dataBR, horaCurta, rotulo } from '../util.js'

/** Tela 04 - Painel do paciente, com duas abas. */
export default function Paciente() {
  const navegar = useNavigate()
  const [aba, setAba] = useState('Agendar consulta')
  const [mensagem, setMensagem] = useState(null)

  function encerrar() {
    sair()
    navegar('/')
  }

  return (
    <div className="pagina">
      <header className="topo">
        <div className="marca">Nutri<span>Flow</span></div>
        <nav>
          {['Agendar consulta', 'Meus agendamentos'].map((nome) => (
            <button key={nome} className={aba === nome ? 'ativa' : ''} onClick={() => { setAba(nome); setMensagem(null) }}>{nome}</button>
          ))}
        </nav>
        <div className="usuario">
          <span>{sessao()?.nome}</span>
          <button onClick={encerrar}>Sair</button>
        </div>
      </header>
      <main className="conteudo estreito">
        <Aviso tipo="sucesso">{mensagem}</Aviso>
        {aba === 'Agendar consulta'
          ? <Agendar aoSolicitar={(texto) => { setMensagem(texto); setAba('Meus agendamentos') }} />
          : <MeusAgendamentos />}
      </main>
    </div>
  )
}

/** Tela 04.1 - Solicitar consulta: data, horario e origem (obrigatoria em todo agendamento). */
function Agendar({ aoSolicitar }) {
  const [hoje, setHoje] = useState('')
  const [data, setData] = useState('')
  const [horarios, setHorarios] = useState(null)
  const [hora, setHora] = useState('')
  const [origem, setOrigem] = useState('')
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api('/info').then((info) => setHoje(info.hoje)).catch(() => {})
  }, [])

  async function buscarHorarios(dia) {
    setHorarios(null)
    setHora('')
    if (!dia) return
    try {
      setHorarios(await api('/paciente/horarios?data=' + dia))
      setErro(null)
    } catch (falha) {
      setErro(falha.message)
    }
  }

  function escolherData(dia) {
    setData(dia)
    buscarHorarios(dia)
  }

  async function solicitar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await api('/paciente/agendamentos', { method: 'POST', body: { data, hora, origem } })
      aoSolicitar(`Solicitação enviada para ${dataBR(data)} às ${horaCurta(hora)}. Aguarde a confirmação do nutricionista.`)
    } catch (falha) {
      setErro(falha.message)
      buscarHorarios(data) // o horario pode ter sido ocupado por outra pessoa: atualiza a lista
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="painel" onSubmit={solicitar}>
      <h2>Agendar consulta</h2>
      <label>1. Escolha a data
        <input type="date" required min={hoje} value={data} onChange={(e) => escolherData(e.target.value)} />
      </label>

      <fieldset>
        <legend>2. Escolha o horário (consultas de 60 minutos)</legend>
        {!data && <p className="suave">Selecione uma data para ver os horários disponíveis.</p>}
        {data && horarios === null && !erro && <p className="suave">Carregando horários…</p>}
        {horarios?.length === 0 && <p className="suave">Não há horários disponíveis nesta data. Escolha outro dia.</p>}
        <div className="horarios">
          {horarios?.map((h) => (
            <button type="button" key={h} aria-pressed={hora === h} className={hora === h ? 'ativa' : ''} onClick={() => setHora(h)}>
              {horaCurta(h)}
            </button>
          ))}
        </div>
      </fieldset>

      <label>3. Como chegou até este atendimento?
        <select required value={origem} onChange={(e) => setOrigem(e.target.value)}>
          <option value="">Selecione</option>
          {ORIGENS.map((o) => <option key={o} value={o}>{rotulo(o)}</option>)}
        </select>
      </label>

      <Aviso tipo="erro">{erro}</Aviso>
      <button className="primario" disabled={!data || !hora || !origem || enviando}>
        {enviando ? 'Enviando…' : 'Solicitar agendamento'}
      </button>
    </form>
  )
}

/** Tela 04.2 - Historico do proprio paciente. Somente leitura: nao ha cancelamento no MVP. */
function MeusAgendamentos() {
  const carga = useCarregar(() => api('/paciente/agendamentos'))
  return (
    <section className="painel">
      <h2>Meus agendamentos</h2>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Você ainda não solicitou nenhuma consulta."
            linhas={lista}
            colunas={[
              { titulo: 'Data', celula: (a) => dataBR(a.data) },
              { titulo: 'Horário', celula: (a) => horaCurta(a.hora) },
              { titulo: 'Status', celula: (a) => <Selo status={a.status} /> },
            ]}
          />
        )}
      </Estado>
      <p className="suave">Pendente: aguardando o nutricionista. Confirmado: consulta aceita. Negado: solicite outro horário.</p>
    </section>
  )
}
