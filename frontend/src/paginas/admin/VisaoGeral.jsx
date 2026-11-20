import { api } from '../../api.js'
import { Estado, Indicador, SeletorMes, useCarregar } from '../../componentes.jsx'
import { dataHoraBR, moeda, porcento, rotulo, variacao } from '../../util.js'

/** Aba 1 - Visao Geral: indicadores das quatro areas e a conclusao da ultima analise de IA do mes. */
export default function VisaoGeral({ periodo, setPeriodo, irParaRelatorios }) {
  const carga = useCarregar(() => api(`/admin/dashboard?ano=${periodo.ano}&mes=${periodo.mes}`), [periodo.ano, periodo.mes])

  return (
    <>
      <SeletorMes periodo={periodo} onMudar={setPeriodo} />
      <Estado carga={carga}>
        {(d) => {
          const fin = d.financeiro
          const mkt = d.marketing.totais
          const mktAnt = d.marketing.anterior
          let conclusao = null
          try {
            conclusao = d.ultimoRelatorio ? JSON.parse(d.ultimoRelatorio.conteudo).conclusaoGeral : null
          } catch {
            conclusao = null
          }
          return (
            <>
              <section className="bloco">
                <h2>Financeiro</h2>
                <div className="grade">
                  <Indicador titulo="Receita total" valor={moeda(fin.receita)} delta={variacao(fin.receita, fin.anterior?.receita)} />
                  <Indicador titulo="Despesas totais" valor={moeda(fin.despesas)} delta={variacao(fin.despesas, fin.anterior?.despesas)} bomQuandoSobe={false} />
                  <Indicador titulo="Saldo do período" valor={moeda(fin.saldo)} delta={variacao(fin.saldo, fin.anterior?.saldo)} />
                </div>
              </section>

              <section className="bloco">
                <h2>Marketing</h2>
                <div className="grade">
                  <Indicador titulo="Investimento total" valor={moeda(mkt.investimento)} delta={variacao(mkt.investimento, mktAnt?.investimento)} bomQuandoSobe={false} />
                  <Indicador titulo="Conversões" valor={mkt.conversoes} delta={variacao(mkt.conversoes, mktAnt?.conversoes)} nota="Consultas confirmadas atribuídas a um canal" />
                  <Indicador titulo="Receita atribuída aos canais" valor={moeda(mkt.receitaAtribuida)} delta={variacao(mkt.receitaAtribuida, mktAnt?.receitaAtribuida)} />
                  <Indicador titulo="Custo por conversão (CPA)" valor={moeda(mkt.cpa)} />
                  <Indicador titulo="ROI simplificado" valor={porcento(mkt.roi)} />
                </div>
              </section>

              <section className="bloco">
                <h2>Agenda</h2>
                <div className="grade">
                  <Indicador titulo="Consultas confirmadas" valor={d.agenda.consultasConfirmadas} />
                  <Indicador titulo="Horários ocupados" valor={d.agenda.horariosOcupados} nota="Pendentes e confirmados" />
                  <Indicador titulo="Horários disponíveis" valor={d.agenda.horariosDisponiveis} />
                  <Indicador titulo="Taxa de ocupação" valor={porcento(d.agenda.taxaOcupacao)} />
                  <Indicador
                    titulo="Menor procura"
                    valor={d.agenda.diaMenorProcura ? rotulo(d.agenda.diaMenorProcura) : 'Sem dados'}
                    nota={d.agenda.horarioMenorProcura ? 'Horário mais vazio: ' + d.agenda.horarioMenorProcura : null}
                  />
                </div>
              </section>

              <section className="bloco">
                <h2>Retenção</h2>
                <div className="grade">
                  <Indicador titulo="Pacientes sem retorno há mais de 45 dias" valor={d.retencao.totalAlertas} nota="Calculado na data de referência, não no mês escolhido" />
                  <Indicador titulo="Já passaram de 60 dias" valor={d.retencao.acimaDe60Dias} />
                </div>
              </section>

              <section className="bloco ia">
                <h2>Conclusão geral da IA</h2>
                {conclusao ? (
                  <>
                    <p>{conclusao}</p>
                    <p className="suave">
                      Análise gerada em {dataHoraBR(d.ultimoRelatorio.geradoEm)}. Ela não se atualiza sozinha: se os registros mudaram depois, gere uma nova.
                    </p>
                  </>
                ) : (
                  <p className="suave">Ainda não há análise de IA para este mês.</p>
                )}
                <button className="primario" onClick={irParaRelatorios}>{conclusao ? 'Ver relatório completo' : 'Solicitar análise da IA'}</button>
              </section>
            </>
          )
        }}
      </Estado>
    </>
  )
}
