import { useState } from 'react'
import { api } from '../../api.js'
import { Aviso, Estado, SeletorMes, Subabas, Tabela, useCarregar } from '../../componentes.jsx'
import { dataHoraBR, moeda, porcento, rotulo } from '../../util.js'

const SUBABAS = ['Relatórios e indicadores', 'Análise com IA']

/** Aba 7 - Relatorios e Inteligencia Artificial. */
export default function Relatorios({ periodo, setPeriodo }) {
  const [sub, setSub] = useState(SUBABAS[1])
  return (
    <>
      <Subabas itens={SUBABAS} atual={sub} onMudar={setSub} />
      <SeletorMes periodo={periodo} onMudar={setPeriodo} />
      {sub === 'Relatórios e indicadores' ? <Indicadores periodo={periodo} /> : <AnaliseIA periodo={periodo} />}
    </>
  )
}

const consulta = (p) => `ano=${p.ano}&mes=${p.mes}`

/** 7.2 - Os mesmos numeros que sao enviados a IA, em formato de relatorio. */
function Indicadores({ periodo }) {
  const carga = useCarregar(() => api('/admin/indicadores?' + consulta(periodo)), [periodo.ano, periodo.mes])
  return (
    <Estado carga={carga}>
      {(d) => (
        <>
          <section className="painel">
            <h3>Financeiro</h3>
            <Tabela
              linhas={[
                { id: 1, nome: 'Receitas', atual: moeda(d.financeiro.receita), anterior: d.financeiro.anterior && moeda(d.financeiro.anterior.receita) },
                { id: 2, nome: 'Despesas', atual: moeda(d.financeiro.despesas), anterior: d.financeiro.anterior && moeda(d.financeiro.anterior.despesas) },
                { id: 3, nome: 'Saldo', atual: moeda(d.financeiro.saldo), anterior: d.financeiro.anterior && moeda(d.financeiro.anterior.saldo) },
              ]}
              colunas={[
                { titulo: 'Indicador', celula: (l) => l.nome },
                { titulo: 'Mês analisado', numero: true, celula: (l) => l.atual },
                { titulo: 'Mês anterior', numero: true, celula: (l) => l.anterior || 'Sem dados' },
              ]}
            />
          </section>
          <section className="painel">
            <h3>Marketing</h3>
            <Tabela
              linhas={[...d.marketing.canais, d.marketing.totais]}
              colunas={[
                { titulo: 'Canal', celula: (c) => rotulo(c.canal) },
                { titulo: 'Investimento', numero: true, celula: (c) => moeda(c.investimento) },
                { titulo: 'Conversões', numero: true, celula: (c) => c.conversoes },
                { titulo: 'Receita atribuída', numero: true, celula: (c) => moeda(c.receitaAtribuida) },
                { titulo: 'CPA', numero: true, celula: (c) => moeda(c.cpa) },
                { titulo: 'ROI', numero: true, celula: (c) => porcento(c.roi) },
              ]}
            />
          </section>
          <section className="painel">
            <h3>Agenda</h3>
            <p>
              {d.agenda.consultasConfirmadas} consulta(s) confirmada(s) · {d.agenda.horariosOcupados} de {d.agenda.horariosTotais} horários ocupados ·
              ocupação de {porcento(d.agenda.taxaOcupacao)}
            </p>
            <Tabela
              vazio="Sem horários de atendimento neste mês."
              linhas={d.agenda.porDiaSemana.map((f, i) => ({ ...f, id: i }))}
              colunas={[
                { titulo: 'Dia da semana', celula: (f) => rotulo(f.nome) },
                { titulo: 'Horários', numero: true, celula: (f) => f.total },
                { titulo: 'Ocupados', numero: true, celula: (f) => f.ocupados },
                { titulo: 'Ocupação', numero: true, celula: (f) => porcento(f.taxa) },
              ]}
            />
            <Tabela
              vazio=""
              linhas={d.agenda.porHorario.map((f, i) => ({ ...f, id: i }))}
              colunas={[
                { titulo: 'Horário', celula: (f) => f.nome },
                { titulo: 'Horários', numero: true, celula: (f) => f.total },
                { titulo: 'Ocupados', numero: true, celula: (f) => f.ocupados },
                { titulo: 'Ocupação', numero: true, celula: (f) => porcento(f.taxa) },
              ]}
            />
          </section>
          <section className="painel">
            <h3>Retenção</h3>
            <p>
              {d.retencao.totalAlertas} paciente(s) sem retorno há mais de 45 dias, dos quais {d.retencao.acimaDe60Dias} já passaram de 60 dias.
            </p>
          </section>
        </>
      )}
    </Estado>
  )
}

/** 7.3 - Analise sob demanda. Cada analise fica guardada com a data em que foi gerada. */
function AnaliseIA({ periodo }) {
  const [escolhido, setEscolhido] = useState(0)
  const [gerando, setGerando] = useState(false)
  const [erro, setErro] = useState(null)
  const carga = useCarregar(() => api('/admin/relatorios?' + consulta(periodo)), [periodo.ano, periodo.mes])

  async function gerar() {
    setErro(null)
    setGerando(true)
    try {
      await api('/admin/relatorios?' + consulta(periodo), { method: 'POST' })
      setEscolhido(0)
      carga.recarregar()
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setGerando(false)
    }
  }

  return (
    <>
      <section className="painel">
        <p className="suave">
          O sistema calcula os indicadores do mês e envia à IA apenas números agregados, sem nomes de pacientes. A IA interpreta e recomenda;
          ela não altera agendamentos, não aplica descontos e não envia mensagens. A análise só é gerada quando solicitada.
        </p>
        <div className="linha-acoes">
          <button className="primario" onClick={gerar} disabled={gerando}>
            {gerando ? 'Gerando análise… pode levar até um minuto' : carga.dados?.length ? 'Gerar nova análise com IA' : 'Gerar análise com IA'}
          </button>
        </div>
        <Aviso tipo="erro">{erro}</Aviso>
      </section>
      <Estado carga={carga}>
        {(lista) => {
          if (!lista.length) return <p className="suave vazio">Ainda não há análise de IA para este mês.</p>
          const relatorio = lista[Math.min(escolhido, lista.length - 1)]
          return (
            <>
              {lista.length > 1 && (
                <label>Análises deste mês
                  <select value={escolhido} onChange={(e) => setEscolhido(Number(e.target.value))}>
                    {lista.map((r, i) => <option key={r.id} value={i}>{dataHoraBR(r.geradoEm)}{i === 0 ? ' (mais recente)' : ''}</option>)}
                  </select>
                </label>
              )}
              <Relatorio relatorio={relatorio} />
            </>
          )
        }}
      </Estado>
    </>
  )
}

function Relatorio({ relatorio }) {
  let r
  try {
    r = JSON.parse(relatorio.conteudo)
  } catch {
    return <Aviso tipo="erro">Esta análise foi gravada em um formato que não pôde ser lido. Gere uma nova.</Aviso>
  }
  const secoes = [
    ['1. Resumo executivo', r.resumoExecutivo],
    ['2. Análise financeira', r.analiseFinanceira],
    ['3. Análise de marketing', r.analiseMarketing],
    ['4. Agenda e ocupação', r.analiseAgenda],
    ['5. Retenção', r.analiseRetencao],
    ['6. Cruzamento de informações', r.cruzamentos],
  ]
  return (
    <article className="painel relatorio">
      <p className="suave">
        Análise gerada em {dataHoraBR(relatorio.geradoEm)}. Ela reflete os dados daquele momento e não se atualiza sozinha.
      </p>
      {secoes.map(([titulo, texto]) => (
        <section key={titulo}>
          <h3>{titulo}</h3>
          <p>{texto || 'A IA não devolveu conteúdo para esta seção.'}</p>
        </section>
      ))}
      <section>
        <h3>7. Projeção para os próximos três meses</h3>
        {r.projecao?.disponivel === false && <p className="suave">Sem histórico suficiente para uma projeção confiável.</p>}
        <p>{r.projecao?.texto}</p>
        {r.projecao?.disponivel && <p className="suave">Os valores acima são estimativas, não resultados garantidos.</p>}
      </section>
      <section>
        <h3>8. Recomendações priorizadas</h3>
        {Array.isArray(r.recomendacoes) && r.recomendacoes.length ? (
          <ol>
            {[...r.recomendacoes].sort((a, b) => (a.prioridade ?? 99) - (b.prioridade ?? 99)).map((rec, i) => (
              <li key={i}>
                <strong>{rec.area}:</strong> {rec.acao}
                {rec.justificativa && <div className="suave">Motivo: {rec.justificativa}</div>}
              </li>
            ))}
          </ol>
        ) : (
          <p className="suave">Nenhuma recomendação devolvida.</p>
        )}
        <p className="suave">A decisão de aplicar qualquer recomendação é sempre do nutricionista.</p>
      </section>
      <section>
        <h3>9. Limitações e qualidade dos dados</h3>
        {Array.isArray(r.limitacoes) && r.limitacoes.length ? (
          <ul>{r.limitacoes.map((l, i) => <li key={i}>{l}</li>)}</ul>
        ) : (
          <p className="suave">Nenhuma limitação apontada.</p>
        )}
      </section>
    </article>
  )
}
