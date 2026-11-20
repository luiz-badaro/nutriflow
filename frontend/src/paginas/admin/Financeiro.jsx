import { useEffect, useState } from 'react'
import { Bar, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../../api.js'
import { Aviso, Estado, Indicador, SeletorMes, Subabas, Tabela, useCarregar } from '../../componentes.jsx'
import { CATEGORIAS, dataBR, horaCurta, mesCurto, moeda, rotulo, variacao } from '../../util.js'

const SUBABAS = ['Resumo financeiro', 'Receitas', 'Despesas', 'Valor da consulta']

/** Aba 4 - Financeiro. */
export default function Financeiro({ periodo, setPeriodo, hoje }) {
  const [sub, setSub] = useState(SUBABAS[0])
  return (
    <>
      <Subabas itens={SUBABAS} atual={sub} onMudar={setSub} />
      {sub !== 'Valor da consulta' && <SeletorMes periodo={periodo} onMudar={setPeriodo} />}
      {sub === 'Resumo financeiro' && <Resumo periodo={periodo} />}
      {sub === 'Receitas' && <Receitas periodo={periodo} />}
      {sub === 'Despesas' && <Despesas periodo={periodo} hoje={hoje} />}
      {sub === 'Valor da consulta' && <ValorConsulta />}
    </>
  )
}

const consulta = (p) => `ano=${p.ano}&mes=${p.mes}`

function Resumo({ periodo }) {
  const carga = useCarregar(() => api('/admin/financeiro/resumo?' + consulta(periodo)), [periodo.ano, periodo.mes])
  return (
    <Estado carga={carga}>
      {(f) => (
        <>
          <div className="grade">
            <Indicador titulo="Receita total" valor={moeda(f.receita)} delta={variacao(f.receita, f.anterior?.receita)} nota="Todas as consultas confirmadas, de qualquer origem" />
            <Indicador titulo="Despesas totais" valor={moeda(f.despesas)} delta={variacao(f.despesas, f.anterior?.despesas)} bomQuandoSobe={false} />
            <Indicador titulo="Saldo (receitas − despesas)" valor={moeda(f.saldo)} delta={variacao(f.saldo, f.anterior?.saldo)} />
          </div>
          <section className="painel">
            <h3>Evolução mensal</h3>
            <div className="grafico">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={f.evolucao.map((m) => ({ ...m, nome: mesCurto(m.mes) }))}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="nome" />
                  <YAxis tickFormatter={(v) => 'R$ ' + v} width={80} />
                  <Tooltip formatter={(v) => moeda(v)} />
                  <Legend />
                  <Bar dataKey="receita" name="Receitas" fill="#1f7a4d" />
                  <Bar dataKey="despesas" name="Despesas" fill="#c2553f" />
                  <Line dataKey="saldo" name="Saldo" stroke="#1d3557" strokeWidth={2} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </section>
        </>
      )}
    </Estado>
  )
}

/** 4.2 - Receitas: somente leitura, geradas automaticamente na confirmacao de cada consulta. */
function Receitas({ periodo }) {
  const carga = useCarregar(() => api('/admin/receitas?' + consulta(periodo)), [periodo.ano, periodo.mes])
  return (
    <section className="painel">
      <p className="suave">As receitas nascem da confirmação das consultas. Não há lançamento manual nem pagamento online.</p>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Nenhuma receita neste mês."
            linhas={lista}
            colunas={[
              { titulo: 'Paciente', celula: (r) => r.paciente },
              { titulo: 'Data da receita', celula: (r) => dataBR(r.data) },
              { titulo: 'Consulta relacionada', celula: (r) => `${dataBR(r.data)} às ${horaCurta(r.hora)}` },
              { titulo: 'Origem do agendamento', celula: (r) => rotulo(r.origem) },
              { titulo: 'Valor', numero: true, celula: (r) => moeda(r.valor) },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}

/** 4.3 - Despesas: cadastro manual, com edicao e exclusao. */
function Despesas({ periodo, hoje }) {
  const vazio = { descricao: '', categoria: '', valor: '', data: hoje }
  const [form, setForm] = useState(null) // null = lista; objeto = formulario aberto
  const [erro, setErro] = useState(null)
  const carga = useCarregar(() => api('/admin/despesas?' + consulta(periodo)), [periodo.ano, periodo.mes])

  async function salvar(e) {
    e.preventDefault()
    setErro(null)
    try {
      const corpo = { descricao: form.descricao, categoria: form.categoria, valor: Number(form.valor), data: form.data }
      if (form.id) await api('/admin/despesas/' + form.id, { method: 'PUT', body: corpo })
      else await api('/admin/despesas', { method: 'POST', body: corpo })
      setForm(null)
      carga.recarregar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  async function remover(d) {
    if (!window.confirm(`Excluir a despesa "${d.descricao}"?`)) return
    try {
      await api('/admin/despesas/' + d.id, { method: 'DELETE' })
      carga.recarregar()
    } catch (falha) {
      setErro(falha.message)
    }
  }

  if (form) {
    return (
      <form className="painel estreito" onSubmit={salvar}>
        <h3>{form.id ? 'Editar despesa' : 'Adicionar despesa'}</h3>
        <label>Descrição<input required value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></label>
        <label>Categoria
          <select required value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>
            <option value="">Selecione</option>
            {CATEGORIAS.map((c) => <option key={c} value={c}>{rotulo(c)}</option>)}
          </select>
        </label>
        <label>Valor (R$)<input type="number" required min="0.01" step="0.01" value={form.valor} onChange={(e) => setForm({ ...form, valor: e.target.value })} /></label>
        <label>Data<input type="date" required value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} /></label>
        <Aviso tipo="erro">{erro}</Aviso>
        <div className="linha-acoes">
          <button type="button" onClick={() => { setForm(null); setErro(null) }}>Voltar</button>
          <button className="primario">Salvar despesa</button>
        </div>
      </form>
    )
  }

  return (
    <section className="painel">
      <div className="linha-acoes">
        <button className="primario" onClick={() => setForm(vazio)}>Adicionar despesa</button>
      </div>
      <Aviso tipo="erro">{erro}</Aviso>
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Nenhuma despesa neste mês."
            linhas={lista}
            colunas={[
              { titulo: 'Data', celula: (d) => dataBR(d.data) },
              { titulo: 'Descrição', celula: (d) => d.descricao },
              { titulo: 'Categoria', celula: (d) => rotulo(d.categoria) },
              { titulo: 'Valor', numero: true, celula: (d) => moeda(d.valor) },
              {
                titulo: '',
                celula: (d) => (
                  <span className="acoes">
                    <button className="pequeno" onClick={() => setForm({ ...d, valor: String(d.valor) })}>Editar</button>
                    <button className="perigo pequeno" onClick={() => remover(d)}>Excluir</button>
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

/** 4.4 - Valor padrao da consulta. So vale para novos agendamentos. */
function ValorConsulta() {
  const [valor, setValor] = useState('')
  const [erro, setErro] = useState(null)
  const [mensagem, setMensagem] = useState(null)

  useEffect(() => {
    api('/admin/valor-consulta').then((r) => setValor(String(r.valor))).catch((falha) => setErro(falha.message))
  }, [])

  async function salvar(e) {
    e.preventDefault()
    setErro(null)
    setMensagem(null)
    try {
      const r = await api('/admin/valor-consulta', { method: 'PUT', body: { valor: Number(valor) } })
      setMensagem(`Valor salvo: ${moeda(r.valor)}. Agendamentos já existentes mantêm o valor que tinham.`)
    } catch (falha) {
      setErro(falha.message)
    }
  }

  return (
    <form className="painel estreito" onSubmit={salvar}>
      <label>Valor da consulta (R$)
        <input type="number" required min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} />
      </label>
      <p className="suave">O valor é gravado em cada novo agendamento. Mudar o preço não altera o histórico financeiro.</p>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{mensagem}</Aviso>
      <button className="primario">Salvar</button>
    </form>
  )
}
