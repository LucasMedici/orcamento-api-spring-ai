package br.com.orcamentoapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import br.com.orcamentoapi.ia.FerramentasFinanceiras;

/**
 * Monta o ChatClient uma única vez, já com o prompt de sistema e as ferramentas registradas.
 * Assim, toda conversa com a IA segue as mesmas regras.
 */
@Configuration
public class AssistenteConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 @Value("classpath:prompts/assistente-financeiro.st") Resource promptSistema,
                                 FerramentasFinanceiras ferramentas) throws IOException {
        return builder
                .defaultSystem(promptSistema.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(ferramentas)
                .build();
    }
}
