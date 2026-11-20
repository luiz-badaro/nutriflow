import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../../api.js'
import { Aviso, Distribuicao, Estado, Indicador, SeletorMes, Subabas, Tabela, useCarregar } from '../../componentes.jsx'
import { CANAIS, MESES, moeda, porcento, rotulo, variacao } from '../../util.js'

const SUBABAS = ['Resumo de Marketing', 'Investimentos', 'Desempenho por canal', 'Perfil do público']

/** Aba 5 - Marketing. Conversao = consulta confirmada com origem em um canal; Retorno nunca conta. */
export default function Marketing({ periodo, setPeriodo }) {
  const [sub, setSub] = useState(SUBABAS[0])
  const carga = useCarregar(() => api(`/admin/marketing/resumo?ano=${periodo.ano}&mes=${periodo.mes}`), [periodo.ano, periodo.mes])
  return (
    <>
      <Subabas itens={SUBABAS} atual={sub} onMudar={setSub} />
      <SeletorMes periodo={periodo} onMudar={setPeriodo} />
      {sub === 'Investimentos' ? (
        <Investimentos periodo={periodo} aoMudar={carga.recarregar} />
      ) : (
        <Estado carga={carga}>
          {(m) => (
            <>
              {sub === 'Resumo de Marketing' && <Resumo m={m} />}
              {sub === 'Desempenho por canal' && <Desempenho m={m} />}
              {sub === 'Perfil do público' && <Perfil perfil={m.perfil} />}
            </>
          )}
        </Estado>
      )}
    </>
  )
}

function Resumo({ m }) {
  const t = m.totais
  const a = m.anterior
  return (
    <>
      <div className="grade">
        <Indicador titulo="Investimento total" valor={moeda(t.investimento)} delta={variacao(t.investimento, a?.investimento)} bomQuandoSobe={false} />
        <Indicador titulo="Conversões" valor={t.conversoes} delta={variacao(t.conversoes, a?.conversoes)} />
        <Indicador titulo="Receita atribuída ao marketing" valor={moeda(t.receitaAtribuida)} delta={variacao(t.receitaAtribuida, a?.receitaAtribuida)} />
        <Indicador titulo="Custo por conversão (CPA)" valor={moeda(t.cpa)} />
        <Indicador titulo="ROI simplificado" valor={porcento(t.roi)} />
      </div>
      <p className="suave">
        {m.consultasConfirmadas} consulta(s) confirmada(s) no mês: {t.conversoes} conversão(ões) em canais e {m.consultasRetorno} com origem
        Retorno, que não contam como conversão nem têm receita atribuída a canal. Conversões não são pacientes novos: o mesmo paciente pode gerar várias.
      </p>
      <section className="painel">
        <h3>Investimento e receita atribuída por canal</h3>
        <div className="grafico">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={m.canais.map((c) => ({ ...c, nome: rotulo(c.canal) }))}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="nome" />
              <YAxis tickFormatter={(v) => 'R$ ' + v} width={80} />
              <Tooltip formatter={(v) => moeda(v)} />
              <Legend />
              <Bar dataKey="investimento" name="Investimento" fill="#c2553f" />
              <Bar dataKey="receitaAtribuida" name="Receita atribuída" fill="#1f7a4d" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
    </>
  )
}

function Desempenho({ m }) {
  return (
    <section className="painel">
      <Tabela
        linhas={[...m.canais, m.totais]}
        colunas={[
          { titulo: 'Canal', celula: (c) => (c.canal === 'TOTAL' ? <strong>Total</strong> : rotulo(c.canal)) },
          { titulo: 'Investimento', numero: true, celula: (c) => moeda(c.investimento) },
          { titulo: 'Conversões', numero: true, celula: (c) => c.conversoes },
          { titulo: 'Receita atribuída', numero: true, celula: (c) => moeda(c.receitaAtribuida) },
          { titulo: 'CPA', numero: true, celula: (c) => moeda(c.cpa) },
          { titulo: 'ROI simplificado', numero: true, celula: (c) => porcento(c.roi) },
        ]}
      />
      <p className="suave">
        CPA = investimento ÷ conversões (não calculável sem conversões). ROI = ((receita atribuída − investimento) ÷ investimento) × 100
        (não calculável sem investimento). A atribuição mostra a origem informada pelo paciente e não prova que o canal causou a consulta.
      </p>
    </section>
  )
}

function Perfil({ perfil }) {
  const [canal, setCanal] = useState('GERAL')
  const dados = canal === 'GERAL' ? perfil.geral : perfil.porCanal[canal]
  return (
    <section className="painel">
      <label>Recorte
        <select value={canal} onChange={(e) => setCanal(e.target.value)}>
          <option value="GERAL">Todos os pacientes cadastrados</option>
          {CANAIS.map((c) => <option key={c} value={c}>Conversões do canal {rotulo(c)}</option>)}
        </select>
      </label>
      <p className="suave">
        {canal === 'GERAL'
          ? `${dados.total} paciente(s) cadastrado(s).`
          : `${dados.total} consulta(s) confirmada(s) atribuída(s) a este canal no mês. Conta consultas, não pacientes únicos; Retorno fica fora.`}
      </p>
      <div className="grade">
        <Distribuicao titulo="Faixa etária" contagem={dados.faixaEtaria} />
        <Distribuicao titulo="Sexo" contagem={dados.sexo} />
        <Distribuicao titulo="Objetivo nutricional" contagem={dados.objetivo} />
      </div>
    </section>
  )
}

/** 5.2 - Investimentos: registro manual por canal e mes de referencia. */
function Investimentos({ periodo, aoMudar }) {
  const vazio = { canal: '', ano: periodo.ano, mes: periodo.mes, valor: '' }
  const [form, setForm] = useState(null)
  const [erro, setErro] = useState(null)
  const carga = useCarregar(() => api(`/admin/investimentos?ano=${periodo.ano}&mes=${periodo.mes}`), [periodo.ano, periodo.mes])

  function atualizar() {
    carga.recarregar()
    aoMudar()
  }

  async function salvar(e) {
    e.preventDefault()
    setErro(null)
    try {
      const corpo = { canal: form.canal, ano: Number(form.ano), mes: Number(form.mes), valor: Number(form.valor) }
      if (form.id) await api('/admin/investimentos/' + form.id, { method: 'PUT', body: corpo })
      else await api('/admin/investimentos', { method: 'POST', body: corpo })
      setForm(null)
      atualizar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  async function remover(inv) {
    if (!window.confirm(`Excluir o investimento de ${moeda(inv.valor)} em ${rotulo(inv.canal)}?`)) return
    try {
      await api('/admin/investimentos/' + inv.id, { method: 'DELETE' })
      atualizar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  if (form) {
    return (
      <form className="painel estreito" onSubmit={salvar}>
        <h3>{form.id ? 'Editar investimento' : 'Adicionar investimento'}</h3>
        <label>Canal
          <select required value={form.canal} onChange={(e) => setForm({ ...form, canal: e.target.value })}>
            <option value="">Selecione</option>
            {CANAIS.map((c) => <option key={c} value={c}>{rotulo(c)}</option>)}
          </select>
        </label>
        <div className="duas-colunas">
          <label>Mês de referência
            <select value={form.mes} onChange={(e) => setForm({ ...form, mes: e.target.value })}>
              {MESES.map((nome, i) => <option key={nome} value={i + 1}>{nome}</option>)}
            </select>
          </label>
          <label>Ano<input type="number" required min="2000" max="2100" value={form.ano} onChange={(e) => setForm({ ...form, ano: e.target.value })} /></label>
        </div>
        <label>Valor (R$)<input type="number" required min="0.01" step="0.01" value={form.valor} onChange={(e) => setForm({ ...form, valor: e.target.value })} /></label>
        <Aviso tipo="erro">{erro}</Aviso>
        <div className="linha-acoes">
          <button type="button" onClick={() => { setForm(null); setErro(null) }}>Voltar</button>
          <button className="primario">Salvar investimento</button>
        </div>
      </form>
    )
  }

  return (
    <section className="painel">
      <div className="linha-acoes">
        <button className="primario" onClick={() => setForm(vazio)}>Adicionar investimento</button>
      </div>
      <Aviso tipo="erro">{erro}</Aviso>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Nenhum investimento registrado neste mês."
            linhas={lista}
            colunas={[
              { titulo: 'Canal', celula: (i) => rotulo(i.canal) },
              { titulo: 'Período', celula: (i) => `${MESES[i.mes - 1]} de ${i.ano}` },
              { titulo: 'Valor', numero: true, celula: (i) => moeda(i.valor) },
              {
                titulo: '',
                celula: (i) => (
                  <span className="acoes">
                    <button className="pequeno" onClick={() => setForm({ ...i, valor: String(i.valor) })}>Editar</button>
                    <button className="perigo pequeno" onClick={() => remover(i)}>Excluir</button>
                  </span>
                ),
              },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}
