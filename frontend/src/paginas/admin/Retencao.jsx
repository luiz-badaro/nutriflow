import { useState } from 'react'
import { api } from '../../api.js'
import { Estado, Indicador, Tabela, useCarregar } from '../../componentes.jsx'
import { dataBR } from '../../util.js'
import { Historico } from './Pacientes.jsx'

/** Aba 6 - Retencao: pacientes com mais de 45 dias desde a ultima consulta confirmada. */
export default function Retencao() {
  const [selecionado, setSelecionado] = useState(null)
  const carga = useCarregar(() => api('/admin/retencao'))

  if (selecionado) return <Historico pacienteId={selecionado} aoVoltar={() => setSelecionado(null)} />
  return (
    <Estado carga={carga}>
      {(r) => (
        <>
          <div className="grade">
            <Indicador titulo="Sem retorno há mais de 45 dias" valor={r.totalAlertas} nota={`De ${r.pacientesComHistorico} paciente(s) com consulta realizada`} />
            <Indicador titulo="Já passaram de 60 dias" valor={r.acimaDe60Dias} nota="Intervalo que o acompanhamento procura evitar" />
            <Indicador titulo="Data de referência" valor={dataBR(r.dataReferencia)} />
          </div>
          <section className="painel">
            <h3>Pacientes sem retorno</h3>
            <Tabela
              vazio="Nenhum paciente passou de 45 dias sem retorno."
              linhas={r.alertas.map((a) => ({ ...a, id: a.pacienteId }))}
              colunas={[
                { titulo: 'Paciente', celula: (a) => <button className="link" onClick={() => setSelecionado(a.pacienteId)}>{a.nome}</button> },
                { titulo: 'Telefone', celula: (a) => a.telefone },
                { titulo: 'Última consulta', celula: (a) => dataBR(a.ultimaConsulta) },
                { titulo: 'Dias sem retorno', numero: true, celula: (a) => a.diasSemRetorno },
                { titulo: 'Alerta', celula: (a) => <span className={'selo ' + (a.acimaDaMeta ? 'negado' : 'pendente')}>{a.acimaDaMeta ? 'Mais de 60 dias' : 'Mais de 45 dias'}</span> },
              ]}
            />
            <p className="suave">
              Consultas de qualquer origem contam para a retenção. O sistema apenas aponta quem precisa de atenção: o contato é feito pelo
              nutricionista, por fora, e nenhuma mensagem é enviada automaticamente. Clique no nome para ver o histórico.
            </p>
          </section>
        </>
      )}
    </Estado>
  )
}
