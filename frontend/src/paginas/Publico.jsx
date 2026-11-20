import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { api, salvarSessao, sessao } from '../api.js'
import { Aviso } from '../componentes.jsx'
import { OBJETIVOS, SEXOS, rotulo } from '../util.js'

function Moldura({ titulo, children }) {
  return (
    <main className="publico">
      <div className="cartao-publico">
        <div className="marca">Nutri<span>Flow</span></div>
        <h1>{titulo}</h1>
        {children}
      </div>
    </main>
  )
}

/** Tela 01 - Selecao de perfil e login. */
export function Login() {
  const navegar = useNavigate()
  const local = useLocation()
  const [perfil, setPerfil] = useState('PACIENTE')
  const [form, setForm] = useState({ email: '', senha: '' })
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const s = sessao()
  if (s) return <Navigate to={s.perfil === 'PACIENTE' ? '/paciente' : '/admin'} replace />

  async function entrar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      const dados = await api('/auth/login', { method: 'POST', body: { ...form, perfil } })
      salvarSessao(dados)
      navegar(dados.perfil === 'PACIENTE' ? '/paciente' : '/admin')
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Moldura titulo="Entrar">
      <Aviso tipo="sucesso">{local.state?.mensagem}</Aviso>
      <div className="perfis" role="radiogroup" aria-label="Perfil de acesso">
        <button type="button" role="radio" aria-checked={perfil === 'PACIENTE'} className={perfil === 'PACIENTE' ? 'ativa' : ''} onClick={() => setPerfil('PACIENTE')}>
          Sou paciente
        </button>
        <button type="button" role="radio" aria-checked={perfil === 'NUTRICIONISTA'} className={perfil === 'NUTRICIONISTA' ? 'ativa' : ''} onClick={() => setPerfil('NUTRICIONISTA')}>
          Sou nutricionista
        </button>
      </div>
      <form onSubmit={entrar}>
        <label>E-mail
          <input type="email" required autoComplete="username" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </label>
        <label>Senha
          <input type="password" required autoComplete="current-password" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
        </label>
        <Aviso tipo="erro">{erro}</Aviso>
        <button className="primario" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
      </form>
      <div className="links">
        <Link to="/recuperar">Esqueci minha senha</Link>
        {perfil === 'PACIENTE' && <Link to="/cadastro">Criar conta</Link>}
      </div>
    </Moldura>
  )
}

/** Tela 02 - Cadastro do paciente. A origem do contato nao e pedida aqui, so em cada agendamento. */
export function Cadastro() {
  const navegar = useNavigate()
  const [form, setForm] = useState({ nome: '', email: '', telefone: '', idade: '', sexo: '', objetivo: '', senha: '' })
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const campo = (nome) => ({ value: form[nome], onChange: (e) => setForm({ ...form, [nome]: e.target.value }) })

  async function criar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      const r = await api('/auth/cadastro', { method: 'POST', body: { ...form, idade: Number(form.idade) } })
      navegar('/', { state: { mensagem: r.message } })
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Moldura titulo="Criar conta de paciente">
      <form onSubmit={criar}>
        <label>Nome completo<input required {...campo('nome')} /></label>
        <label>E-mail<input type="email" required autoComplete="email" {...campo('email')} /></label>
        <div className="duas-colunas">
          <label>Telefone<input type="tel" required placeholder="(11) 99999-0000" {...campo('telefone')} /></label>
          <label>Idade<input type="number" required min="1" max="120" {...campo('idade')} /></label>
        </div>
        <div className="duas-colunas">
          <label>Sexo
            <select required {...campo('sexo')}>
              <option value="">Selecione</option>
              {SEXOS.map((s) => <option key={s} value={s}>{rotulo(s)}</option>)}
            </select>
          </label>
          <label>Objetivo nutricional
            <select required {...campo('objetivo')}>
              <option value="">Selecione</option>
              {OBJETIVOS.map((o) => <option key={o} value={o}>{rotulo(o)}</option>)}
            </select>
          </label>
        </div>
        <label>Senha (mínimo de 6 caracteres)
          <input type="password" required minLength={6} autoComplete="new-password" {...campo('senha')} />
        </label>
        <Aviso tipo="erro">{erro}</Aviso>
        <button className="primario" disabled={enviando}>{enviando ? 'Criando…' : 'Criar conta'}</button>
      </form>
      <div className="links"><Link to="/">Voltar ao login</Link></div>
    </Moldura>
  )
}

/** Tela 03 - Recuperacao de senha em dois passos: pedir o codigo e definir a nova senha. */
export function Recuperar() {
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [pedido, setPedido] = useState(null)
  const [token, setToken] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [erro, setErro] = useState(null)

  async function pedir(e) {
    e.preventDefault()
    setErro(null)
    try {
      const r = await api('/auth/recuperar', { method: 'POST', body: { email } })
      setPedido(r)
      if (r.codigoDemo) setToken(r.codigoDemo)
    } catch (falha) {
      setErro(falha.message)
    }
  }

  async function redefinir(e) {
    e.preventDefault()
    setErro(null)
    try {
      const r = await api('/auth/redefinir', { method: 'POST', body: { token, novaSenha } })
      navegar('/', { state: { mensagem: r.message } })
    } catch (falha) {
      setErro(falha.message)
    }
  }

  return (
    <Moldura titulo="Recuperar senha">
      {!pedido ? (
        <form onSubmit={pedir}>
          <p className="suave">Informe o e-mail da conta. Um código de recuperação válido por 30 minutos será gerado.</p>
          <label>E-mail da conta<input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
          <Aviso tipo="erro">{erro}</Aviso>
          <button className="primario">Recuperar senha</button>
        </form>
      ) : (
        <form onSubmit={redefinir}>
          <Aviso tipo="info">
            {pedido.message}
            {pedido.codigoDemo
              ? ' Ambiente de demonstração: como o sistema não envia e-mails, o código já foi preenchido abaixo.'
              : ' Peça o código ao responsável pelo sistema.'}
          </Aviso>
          <label>Código de recuperação<input required value={token} onChange={(e) => setToken(e.target.value)} /></label>
          <label>Nova senha (mínimo de 6 caracteres)
            <input type="password" required minLength={6} autoComplete="new-password" value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} />
          </label>
          <Aviso tipo="erro">{erro}</Aviso>
          <button className="primario">Definir nova senha</button>
        </form>
      )}
      <div className="links"><Link to="/">Voltar ao login</Link></div>
    </Moldura>
  )
}
