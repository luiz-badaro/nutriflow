import { Navigate, Route, Routes } from 'react-router-dom'
import { sessao } from './api.js'
import { Cadastro, Login, Recuperar } from './paginas/Publico.jsx'
import Paciente from './paginas/Paciente.jsx'
import Admin from './paginas/admin/Admin.jsx'

/** So deixa entrar quem esta logado com o perfil certo. O backend valida de novo em cada chamada. */
function Protegida({ perfil, children }) {
  const s = sessao()
  if (!s) return <Navigate to="/" replace />
  if (s.perfil !== perfil) return <Navigate to={s.perfil === 'PACIENTE' ? '/paciente' : '/admin'} replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
      <Route path="/recuperar" element={<Recuperar />} />
      <Route path="/paciente" element={<Protegida perfil="PACIENTE"><Paciente /></Protegida>} />
      <Route path="/admin" element={<Protegida perfil="NUTRICIONISTA"><Admin /></Protegida>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
