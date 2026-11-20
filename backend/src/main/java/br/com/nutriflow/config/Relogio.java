package br.com.nutriflow.config;

import br.com.nutriflow.model.Configuracao;
import br.com.nutriflow.repository.Repositorios.ConfiguracaoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Unica fonte de "data atual" do sistema. Todo o backend pergunta a data aqui, nunca a LocalDate.now().
 * No ambiente de demonstracao (nutriflow.demo=true) devolve a data simulada, se houver uma definida.
 * Fora da demonstracao a data simulada e ignorada (RNF-017).
 */
@Component
public class Relogio {
    private final ConfiguracaoRepository configuracoes;
    private final boolean demo;

    public Relogio(ConfiguracaoRepository configuracoes, @Value("${nutriflow.demo:false}") boolean demo) {
        this.configuracoes = configuracoes;
        this.demo = demo;
    }

    public boolean isDemo() { return demo; }

    public LocalDate hoje() {
        if (demo) {
            LocalDate simulada = configuracoes.findById(Configuracao.ID).map(Configuracao::getDataSimulada).orElse(null);
            if (simulada != null) return simulada;
        }
        return LocalDate.now();
    }

    public LocalDateTime agora() {
        return LocalDateTime.of(hoje(), LocalTime.now().withNano(0));
    }
}
