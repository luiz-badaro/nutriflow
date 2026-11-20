import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, sair, sessao } from '../../api.js'
import { Aviso } from '../../componentes.jsx'
import { dataBR } from '../../util.js'
import VisaoGeral from './VisaoGeral.jsx'
import Agenda from './Agenda.jsx'
import Pacientes from './Pacientes.jsx'
import Financeiro from './Financeiro.jsx'
import Marketing from './Marketing.jsx'
import Retencao from './Retencao.jsx'
import Relatorios from './Relatorios.jsx'
import Configuracoes from './Configuracoes.jsx'

const ABAS = ['Visão Geral', 'Agenda', 'Pacientes', 'Financeiro', 'Marketing', 'Retenção', 'Relatórios e IA', 'Configurações']

/** Tela 05 - Dashboard administrativo: menu fixo com as 8 abas. */
export default function Admin() {
  const navegar = useNavigate()
  const [aba, setAba] = useState(ABAS[0])
  const [info, setInfo] = useState(null)
  const [periodo, setPeriodo] = useState(null) // mes escolhido, compartilhado entre as abas
  const [erro, setErro] = useState(null)

  function carregarInfo() {
    return api('/info').then((dados) => {
      setInfo(dados)
      return dados
    })
  }

  useEffect(() => {
    carregarInfo()
      .then((dados) => setPeriodo({ ano: Number(dados.hoje.slice(0, 4)), mes: Number(dados.hoje.slice(5, 7)) }))
      .catch((falha) => setErro(falha.message))
  }, [])

  function encerrar() {
    sair()
    navegar('/')
  }

  const comuns = { periodo, setPeriodo, hoje: info?.hoje }
  return (
    <div className="pagina admin">
      <aside className="lateral">
        <div className="marca">Nutri<span>Flow</span></div>
        <nav>
          {ABAS.map((nome) => (
            <button key={nome} className={aba === nome ? 'ativa' : ''} aria-current={aba === nome ? 'page' : undefined} onClick={() => setAba(nome)}>
              {nome}
            </button>
          ))}
        </nav>
        <div className="usuario">
          <span>{sessao()?.nome}</span>
          <span className="suave">Administrador</span>
          <button onClick={encerrar}>Sair</button>
        </div>
      </aside>
      <main className="conteudo">
        {info?.demo && <div className="faixa-demo">Ambiente de demonstração · data de referência: {dataBR(info.hoje)}</div>}
        <Aviso tipo="erro">{erro}</Aviso>
        <h1>{aba}</h1>
        {periodo && (
          <>
            {aba === 'Visão Geral' && <VisaoGeral {...comuns} irParaRelatorios={() => setAba('Relatórios e IA')} />}
            {aba === 'Agenda' && <Agenda {...comuns} />}
            {aba === 'Pacientes' && <Pacientes />}
            {aba === 'Financeiro' && <Financeiro {...comuns} />}
            {aba === 'Marketing' && <Marketing {...comuns} />}
            {aba === 'Retenção' && <Retencao />}
            {aba === 'Relatórios e IA' && <Relatorios {...comuns} />}
            {aba === 'Configurações' && <Configuracoes aoMudarData={carregarInfo} />}
          </>
        )}
      </main>
    </div>
  )
}
