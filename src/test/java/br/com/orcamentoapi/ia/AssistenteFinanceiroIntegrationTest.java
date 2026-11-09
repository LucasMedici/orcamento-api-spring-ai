package br.com.orcamentoapi.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.model.Transacao;
import br.com.orcamentoapi.repository.TransacaoRepository;

/**
 * Testes de integração com a OpenAI de verdade (gastam créditos).
 * Só rodam quando a variável OPENAI_API_KEY está definida.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class AssistenteFinanceiroIntegrationTest {

    @Autowired
    AssistenteFinanceiro assistente;

    @Autowired
    TransacaoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
            "recording-1.m4a, 80 reais",
            "recording-2.m4a, 40 reais",
            "recording-3.m4a, 120 reais",
            "recording-4.m4a, 90 reais",
            "recording-5.m4a, 200 reais",
            "recording-6.m4a, 60 reais",
    })
    void transcreveAudiosDeGastos(String arquivo, String trechoEsperado) {
        String texto = assistente.transcrever(new ClassPathResource("audio/" + arquivo));

        assertThat(texto).contains(trechoEsperado);
    }

    @Test
    void iaRegistraGastoUsandoAFerramenta() {
        String resposta = assistente.responder("Gastei 80 reais no mercado hoje");

        assertThat(resposta).isNotBlank();
        assertThat(repository.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getValor()).isEqualByComparingTo("80.00");
            assertThat(t.getCategoria()).isEqualTo(Categoria.MERCADO);
        });
    }

    @Test
    void iaNaoInventaValorQuandoEleNaoEhDito() {
        assistente.responder("Fui na farmácia hoje");

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void iaUsaOResumoParaResponderTotais() {
        repository.save(new Transacao("Gasolina", new BigDecimal("200.00"), Categoria.TRANSPORTE));
        repository.save(new Transacao("Mercado", new BigDecimal("95.50"), Categoria.MERCADO));

        String resposta = assistente.responder("Quanto eu gastei no total?");

        assertThat(resposta).containsAnyOf("295", "duzentos e noventa e cinco");
    }
}
