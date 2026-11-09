package br.com.orcamentoapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Sobe o contexto completo. Só roda com a chave da OpenAI configurada. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OrcamentoApiApplicationTests {

    @Test
    void contextoSobe() {
    }
}
