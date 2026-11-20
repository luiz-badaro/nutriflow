import { useState } from 'react'
import { api } from '../../api.js'
import { Estado, Selo, Tabela, useCarregar } from '../../componentes.jsx'
import { dataBR, horaCurta, moeda, rotulo } from '../../util.js'

/** Aba 3 - Pacientes: lista e historico individual. */
export default function Pacientes() {
  const [selecionado, setSelecionado] = useState(null)
  const carga = useCarregar(() => api('/admin/pacientes'))

  if (selecionado) return <Historico pacienteId={selecionado} aoVoltar={() => setSelecionado(null)} />
  return (
    <section className="painel">
      <Estado carga={carga}>
        {(lista) => (
          <Tabela
            vazio="Nenhum paciente cadastrado ainda."
            linhas={lista}
            colunas={[
              { titulo: 'Nome', celula: (p) => <button className="link" onClick={() => setSelecionado(p.id)}>{p.nome}</button> },
              { titulo: 'E-mail', celula: (p) => p.email },
              { titulo: 'Telefone', celula: (p) => p.telefone },
              { titulo: 'Idade', numero: true, celula: (p) => p.idade },
              { titulo: 'Sexo', celula: (p) => rotulo(p.sexo) },
              { titulo: 'Objetivo nutricional', celula: (p) => rotulo(p.objetivo) },
            ]}
          />
        )}
      </Estado>
    </section>
  )
}

/** 3.2 - Historico: cada consulta guarda a propria origem e o proprio valor. Tambem usado na aba Retencao. */
export function Historico({ pacienteId, aoVoltar }) {
  const carga = useCarregar(() => api(`/admin/pacientes/${pacienteId}/historico`), [pacienteId])
  return (
    <section className="painel">
      <button onClick={aoVoltar}>◀ Voltar</button>
      <Estado carga={carga}>
        {(p) => {
          const confirmadas = p.agendamentos.filter((a) => a.status === 'CONFIRMADO')
          const emCanais = confirmadas.filter((a) => a.origem !== 'RETORNO').length
          return (
            <>
              <h2>{p.nome}</h2>
              <p className="suave">
                {p.email} · {p.telefone} · {p.idade} anos · {rotulo(p.sexo)} · {rotulo(p.objetivo)}
              </p>
              <p>{confirmadas.length} consulta(s) confirmada(s), sendo {emCanais} atribuída(s) a canais de marketing.</p>
              <Tabela
                vazio="Este paciente ainda não tem agendamentos."
                linhas={p.agendamentos}
                colunas={[
                  { titulo: 'Data', celula: (a) => dataBR(a.data) },
                  { titulo: 'Horário', celula: (a) => horaCurta(a.hora) },
                  { titulo: 'Origem', celula: (a) => rotulo(a.origem) },
                  { titulo: 'Valor', numero: true, celula: (a) => moeda(a.valor) },
                  { titulo: 'Status', celula: (a) => <Selo status={a.status} /> },
                ]}
              />
            </>
          )
        }}
      </Estado>
    </section>
  )
}
