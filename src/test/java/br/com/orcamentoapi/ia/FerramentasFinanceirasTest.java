package br.com.orcamentoapi.ia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.orcamentoapi.exception.RegraNegocioException;
import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.repository.TransacaoRepository;
import br.com.orcamentoapi.service.TransacaoService;

class FerramentasFinanceirasTest {

    @Test
    void ferramentaDaIaUsaAsMesmasValidacoesDaApi() {
        TransacaoRepository repository = mock(TransacaoRepository.class);
        FerramentasFinanceiras ferramentas = new FerramentasFinanceiras(new TransacaoService(repository));

        // Simula a IA "inventando" um valor zerado quando a pessoa não disse quanto gastou
        assertThatThrownBy(() -> ferramentas.registrarGasto("Mercado", BigDecimal.ZERO, Categoria.MERCADO))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }
}
