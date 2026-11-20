// Comunicacao com o backend. O token de acesso fica no navegador e vai em toda chamada.
const CHAVE = 'nutriflow.sessao'

export function sessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE))
  } catch {
    return null
  }
}

export function salvarSessao(dados) {
  localStorage.setItem(CHAVE, JSON.stringify(dados))
}

export function sair() {
  localStorage.removeItem(CHAVE)
}

export async function api(caminho, { method = 'GET', body } = {}) {
  const s = sessao()
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (s) headers.Authorization = 'Bearer ' + s.token

  let resposta
  try {
    resposta = await fetch('/api' + caminho, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new Error('Não foi possível falar com o servidor. Confira se o backend está ligado.')
  }

  // Token vencido ou invalido: volta para o login.
  if (resposta.status === 401 && s && !caminho.startsWith('/auth')) {
    sair()
    window.location.href = '/'
    throw new Error('Sessão expirada. Faça o login novamente.')
  }
  if (resposta.status === 204) return null

  const texto = await resposta.text()
  let dados = null
  try {
    dados = texto ? JSON.parse(texto) : null
  } catch {
    dados = null
  }
  if (!resposta.ok) throw new Error(dados?.message || 'Erro ' + resposta.status)
  return dados
}
