import { useCallback, useEffect, useState } from 'react'
import { MESES, porcento, rotulo } from './util.js'

/** Busca dados do backend e controla os estados de carregando e erro. */
export function useCarregar(buscar, dependencias = []) {
  const [estado, setEstado] = useState({ dados: null, erro: null, carregando: true })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const executar = useCallback(buscar, dependencias)
  const recarregar = useCallback(() => {
    let ativo = true
    setEstado((e) => ({ ...e, carregando: true, erro: null }))
    executar()
      .then((dados) => ativo && setEstado({ dados, erro: null, carregando: false }))
      .catch((e) => ativo && setEstado({ dados: null, erro: e.message, carregando: false }))
    return () => {
      ativo = false
    }
  }, [executar])
  useEffect(() => recarregar(), [recarregar])
  return { ...estado, recarregar }
}

/** Mostra "Carregando", o erro ou o conteudo. */
export function Estado({ carga, children }) {
  if (carga.erro) return <Aviso tipo="erro">{carga.erro}</Aviso>
  if (carga.carregando && !carga.dados) return <p className="suave">Carregando…</p>
  if (!carga.dados) return null
  return children(carga.dados)
}

export function Aviso({ tipo = 'info', children }) {
  if (!children) return null
  return <div className={'aviso ' + tipo} role={tipo === 'erro' ? 'alert' : 'status'}>{children}</div>
}

export function Subabas({ itens, atual, onMudar }) {
  return (
    <div className="subabas" role="tablist">
      {itens.map((item) => (
        <button key={item} role="tab" aria-selected={item === atual} className={item === atual ? 'ativa' : ''} onClick={() => onMudar(item)}>
          {item}
        </button>
      ))}
    </div>
  )
}

export function SeletorMes({ periodo, onMudar }) {
  const anos = []
  for (let a = periodo.ano - 3; a <= periodo.ano + 1; a++) anos.push(a)
  return (
    <div className="seletor-mes">
      <label>
        Mês
        <select value={periodo.mes} onChange={(e) => onMudar({ ...periodo, mes: Number(e.target.value) })}>
          {MESES.map((nome, i) => (
            <option key={nome} value={i + 1}>{nome}</option>
          ))}
        </select>
      </label>
      <label>
        Ano
        <select value={periodo.ano} onChange={(e) => onMudar({ ...periodo, ano: Number(e.target.value) })}>
          {anos.map((a) => (
            <option key={a} value={a}>{a}</option>
          ))}
        </select>
      </label>
      <span className="suave">Período analisado: <strong>{MESES[periodo.mes - 1]} de {periodo.ano}</strong></span>
    </div>
  )
}

/**
 * Cartao de indicador. "delta" e a variacao em % contra o mes anterior;
 * "bomQuandoSobe" define a cor (despesa subindo e ruim, receita subindo e boa).
 */
export function Indicador({ titulo, valor, delta, bomQuandoSobe = true, nota }) {
  let comparacao = null
  if (delta !== undefined) {
    if (delta === null) comparacao = <span className="suave">Sem mês anterior para comparar</span>
    else {
      const subiu = delta > 0
      const classe = delta === 0 ? 'suave' : subiu === bomQuandoSobe ? 'bom' : 'ruim'
      comparacao = <span className={classe}>{delta === 0 ? '=' : subiu ? '▲' : '▼'} {porcento(Math.abs(delta))} vs. mês anterior</span>
    }
  }
  return (
    <div className="indicador">
      <div className="indicador-titulo">{titulo}</div>
      <div className={'indicador-valor' + (valor === 'Não calculável' ? ' nao-calculavel' : '')}>{valor}</div>
      {comparacao && <div className="indicador-nota">{comparacao}</div>}
      {nota && <div className="indicador-nota suave">{nota}</div>}
    </div>
  )
}

export function Selo({ status }) {
  return <span className={'selo ' + String(status).toLowerCase()}>{rotulo(status)}</span>
}

/** Tabela simples com mensagem para lista vazia. */
export function Tabela({ colunas, linhas, vazio = 'Nenhum registro encontrado.' }) {
  if (!linhas.length) return <p className="suave vazio">{vazio}</p>
  return (
    <div className="tabela-rolagem">
      <table>
        <thead>
          <tr>{colunas.map((c) => <th key={c.titulo} className={c.numero ? 'num' : ''}>{c.titulo}</th>)}</tr>
        </thead>
        <tbody>
          {linhas.map((linha, i) => (
            <tr key={linha.id ?? i}>
              {colunas.map((c) => <td key={c.titulo} className={c.numero ? 'num' : ''}>{c.celula(linha)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** Barras horizontais para as distribuicoes do perfil do publico. */
export function Distribuicao({ titulo, contagem }) {
  const itens = Object.entries(contagem || {})
  const total = itens.reduce((soma, [, n]) => soma + n, 0)
  return (
    <div className="distribuicao">
      <h4>{titulo}</h4>
      {!itens.length && <p className="suave">Sem dados.</p>}
      {itens.map(([nome, n]) => (
        <div key={nome} className="barra-linha">
          <span className="barra-nome">{rotulo(nome)}</span>
          <span className="barra-trilho"><span className="barra-preenchida" style={{ width: (n / total) * 100 + '%' }} /></span>
          <span className="barra-numero">{n}</span>
        </div>
      ))}
    </div>
  )
}
