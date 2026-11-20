import { useState } from 'react'
import { api, sessao } from '../../api.js'
import { Aviso, Estado, useCarregar } from '../../componentes.jsx'
import { dataBR } from '../../util.js'

/** Aba 8 - Configuracoes: dados da conta e a data simulada do ambiente de demonstracao. */
export default function Configuracoes({ aoMudarData }) {
  const carga = useCarregar(() => api('/admin/configuracoes'))
  const [data, setData] = useState('')
  const [erro, setErro] = useState(null)
  const [mensagem, setMensagem] = useState(null)

  async function aplicar(novaData) {
    setErro(null)
    setMensagem(null)
    try {
      await api('/admin/configuracoes/data-simulada', { method: 'PUT', body: { data: novaData } })
      setMensagem(novaData ? `Data simulada aplicada: ${dataBR(novaData)}.` : 'Data simulada removida: o sistema voltou à data real.')
      carga.recarregar()
      aoMudarData()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  return (
    <Estado carga={carga}>
      {(c) => (
        <>
          <section className="painel estreito">
            <h3>Informações administrativas</h3>
            <p>Conta: <strong>{sessao()?.nome}</strong> (nutricionista administrador)</p>
            <p>Ambiente: {c.demo ? 'demonstração acadêmica' : 'produção'}</p>
            <p>Integração com a IA (Gemini): {c.iaConfigurada ? 'chave configurada no servidor' : 'chave não configurada no servidor'}</p>
            <p className="suave">O valor da consulta fica em Financeiro; disponibilidade e bloqueios ficam em Agenda.</p>
          </section>

          {c.demo ? (
            <section className="painel estreito">
              <h3>Data simulada</h3>
              <p>Data usada pelo sistema agora: <strong>{dataBR(c.hoje)}</strong> {c.dataSimulada ? '(simulada)' : '(data real)'}</p>
              <p className="suave">
                Serve para demonstrar alertas de retenção, relatórios mensais e análises sem esperar o tempo passar. Mudar a data não cria
                agendamentos, receitas, despesas nem investimentos; novos registros passam a respeitar a data definida.
              </p>
              <form className="linha-form" onSubmit={(e) => { e.preventDefault(); aplicar(data) }}>
                <label>Nova data simulada<input type="date" required value={data} onChange={(e) => setData(e.target.value)} /></label>
                <button className="primario">Aplicar data simulada</button>
                {c.dataSimulada && <button type="button" onClick={() => aplicar(null)}>Voltar à data real</button>}
              </form>
              <Aviso tipo="erro">{erro}</Aviso>
              <Aviso tipo="sucesso">{mensagem}</Aviso>
            </section>
          ) : (
            <section className="painel estreito">
              <h3>Data simulada</h3>
              <p className="suave">Disponível apenas no ambiente de demonstração.</p>
            </section>
          )}
        </>
      )}
    </Estado>
  )
}
