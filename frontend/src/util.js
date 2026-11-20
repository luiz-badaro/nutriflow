// Formatacao e rotulos usados em todas as telas.

export const moeda = (v) =>
  v === null || v === undefined ? 'Não calculável' : Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

export const porcento = (v) =>
  v === null || v === undefined ? 'Não calculável' : Number(v).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + '%'

export const dataBR = (iso) => (iso ? iso.slice(0, 10).split('-').reverse().join('/') : '')

export const horaCurta = (h) => (h ? h.slice(0, 5) : '')

export const dataHoraBR = (iso) => (iso ? dataBR(iso) + ' às ' + iso.slice(11, 16) : '')

export const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro']

/** "2026-10" vira "out/26", para os eixos dos graficos. */
export const mesCurto = (anoMes) => MESES[Number(anoMes.slice(5, 7)) - 1].slice(0, 3).toLowerCase() + '/' + anoMes.slice(2, 4)

const ROTULOS = {
  INSTAGRAM: 'Instagram', GOOGLE: 'Google', INDICACAO: 'Indicação', RETORNO: 'Retorno', OUTRO: 'Outro', TOTAL: 'Total',
  PENDENTE: 'Pendente', CONFIRMADO: 'Confirmado', NEGADO: 'Negado', DISPONIVEL: 'Disponível', BLOQUEADO: 'Bloqueado',
  FEMININO: 'Feminino', MASCULINO: 'Masculino',
  EMAGRECIMENTO: 'Emagrecimento', GANHO_DE_MASSA: 'Ganho de massa', REEDUCACAO_ALIMENTAR: 'Reeducação alimentar',
  SAUDE: 'Saúde', DESEMPENHO_ESPORTIVO: 'Desempenho esportivo',
  ALUGUEL: 'Aluguel', MATERIAIS: 'Materiais', SOFTWARE: 'Software', IMPOSTOS: 'Impostos', PESSOAL: 'Pessoal', OUTROS: 'Outros',
  MONDAY: 'Segunda', TUESDAY: 'Terça', WEDNESDAY: 'Quarta', THURSDAY: 'Quinta', FRIDAY: 'Sexta', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
}

export const rotulo = (chave) => ROTULOS[chave] ?? chave ?? ''

export const ORIGENS = ['INSTAGRAM', 'GOOGLE', 'INDICACAO', 'RETORNO', 'OUTRO']
export const CANAIS = ['INSTAGRAM', 'GOOGLE', 'INDICACAO', 'OUTRO'] // Retorno nao e canal de marketing
export const SEXOS = ['FEMININO', 'MASCULINO', 'OUTRO']
export const OBJETIVOS = ['EMAGRECIMENTO', 'GANHO_DE_MASSA', 'REEDUCACAO_ALIMENTAR', 'SAUDE', 'DESEMPENHO_ESPORTIVO', 'OUTRO']
export const CATEGORIAS = ['ALUGUEL', 'MATERIAIS', 'SOFTWARE', 'IMPOSTOS', 'PESSOAL', 'OUTROS']
export const DIAS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

/** Variacao percentual entre o mes escolhido e o anterior; null quando nao ha base para comparar. */
export function variacao(atual, anterior) {
  if (anterior === null || anterior === undefined || atual === null || atual === undefined) return null
  if (Number(anterior) === 0) return null
  return ((Number(atual) - Number(anterior)) / Math.abs(Number(anterior))) * 100
}
