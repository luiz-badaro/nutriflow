package br.com.nutriflow.service;

import br.com.nutriflow.config.Relogio;
import br.com.nutriflow.model.RelatorioIA;
import br.com.nutriflow.repository.Repositorios.RelatorioRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * Integracao com a API Gemini (RF-031 a RF-037). O backend calcula os indicadores e envia so numeros
 * agregados, sem nome, e-mail ou telefone de pacientes (RNF-011). A IA apenas interpreta.
 * Qualquer falha da API vira uma mensagem de erro e nada e gravado (RNF-015).
 */
@Service
public class IaService {
    /** Meses com movimento exigidos para a IA poder apresentar projecao (RN-033). */
    public static final int MESES_MINIMOS_PROJECAO = 3;

    private static final String INSTRUCOES = """
            Voce e um analista de gestao de um consultorio de nutricao. Receba os indicadores abaixo, ja calculados
            pelo sistema, e escreva um relatorio interpretativo em portugues do Brasil.

            Regras obrigatorias:
            - Use somente os numeros fornecidos. Nunca invente valores, historicos ou resultados.
            - Indicador com valor null nao pode ser calculado: diga isso, nao apresente numero.
            - "Conversao" e uma consulta confirmada atribuida a um canal. Nao e paciente novo nem paciente unico.
            - Consultas com origem RETORNO nao pertencem a nenhum canal de marketing.
            - Trate relacoes entre indicadores como associacoes observadas, nunca como causa comprovada.
            - Voce so recomenda. Nao diga que enviou mensagens, alterou agendamentos ou aplicou descontos.
            - Se "projecaoPermitida" for false, nao projete valores: explique que falta historico.
              Se for true, apresente a projecao dos proximos tres meses como estimativa e diga em que se baseou.
            - Nao repita todos os numeros do painel; priorize conclusoes.

            Responda apenas com um JSON neste formato:
            {
              "conclusaoGeral": "2 a 3 frases para o painel inicial",
              "resumoExecutivo": "texto",
              "analiseFinanceira": "texto",
              "analiseMarketing": "texto",
              "analiseAgenda": "texto",
              "analiseRetencao": "texto",
              "cruzamentos": "texto",
              "projecao": { "disponivel": true ou false, "texto": "texto" },
              "recomendacoes": [ { "prioridade": 1, "area": "Financeiro|Marketing|Agenda|Retencao", "acao": "texto", "justificativa": "dado que motivou" } ],
              "limitacoes": [ "texto" ]
            }

            Indicadores:
            """;

    private final IndicadoresService indicadores;
    private final RelatorioRepository relatorios;
    private final Relogio relogio;
    private final ObjectMapper json;
    private final String apiKey;
    private final String modelo;
    private final RestClient http;

    public IaService(IndicadoresService indicadores, RelatorioRepository relatorios, Relogio relogio, ObjectMapper json,
                     @Value("${nutriflow.gemini.api-key:}") String apiKey,
                     @Value("${nutriflow.gemini.model}") String modelo) {
        this.indicadores = indicadores;
        this.relatorios = relatorios;
        this.relogio = relogio;
        this.json = json;
        this.apiKey = apiKey;
        this.modelo = modelo;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(90));
        this.http = RestClient.builder().requestFactory(fabrica)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
    }

    public boolean configurada() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<RelatorioIA> listar(int ano, int mes) {
        return relatorios.findByAnoAndMesOrderByGeradoEmDesc(ano, mes);
    }

    /** Dados enviados a IA: indicadores do mes, sem a lista nominal de pacientes, mais as limitacoes conhecidas. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> dadosParaAnalise(YearMonth mes) {
        Map<String, Object> dados = indicadores.tudo(mes);
        Map<String, Object> retencao = new LinkedHashMap<>((Map<String, Object>) dados.get("retencao"));
        retencao.remove("alertas"); // nomes e telefones nao saem do sistema
        dados.put("retencao", retencao);

        List<Map<String, Object>> evolucao = (List<Map<String, Object>>) ((Map<String, Object>) dados.get("financeiro")).get("evolucao");
        long mesesComMovimento = evolucao.stream().filter(m ->
                ((BigDecimal) m.get("receita")).signum() != 0 || ((BigDecimal) m.get("despesas")).signum() != 0).count();
        dados.put("mesesComHistorico", mesesComMovimento);
        dados.put("projecaoPermitida", mesesComMovimento >= MESES_MINIMOS_PROJECAO);
        dados.put("temMesAnteriorParaComparar", ((Map<String, Object>) dados.get("financeiro")).get("anterior") != null);
        return dados;
    }

    @SuppressWarnings("unchecked")
    public RelatorioIA gerar(YearMonth mes) {
        if (!configurada())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave da API Gemini não está configurada no servidor (variável de ambiente GEMINI_API_KEY)");

        String prompt = INSTRUCOES + json.writeValueAsString(dadosParaAnalise(mes));
        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.3));

        String texto;
        try {
            Map<String, Object> resposta = http.post().uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey).body(corpo).retrieve().body(Map.class);
            List<Map<String, Object>> candidatos = (List<Map<String, Object>>) resposta.get("candidates");
            Map<String, Object> conteudo = (Map<String, Object>) candidatos.get(0).get("content");
            texto = (String) ((List<Map<String, Object>>) conteudo.get("parts")).get(0).get("text");
            json.readTree(texto); // confirma que a IA devolveu um JSON valido
        } catch (RestClientResponseException e) {
            boolean limite = e.getStatusCode().value() == 429;
            throw new ResponseStatusException(limite ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY, limite
                    ? "O limite de uso da API Gemini foi atingido. Tente novamente em alguns minutos."
                    : "A API Gemini recusou a solicitação (código " + e.getStatusCode().value() + "). Confira a chave e o modelo configurados.");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar à API Gemini. Verifique a internet e tente de novo.");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A IA devolveu uma resposta inválida. Nenhum dado foi alterado; tente gerar novamente.");
        }
        return relatorios.save(new RelatorioIA(mes.getYear(), mes.getMonthValue(), relogio.agora(), texto));
    }
}
