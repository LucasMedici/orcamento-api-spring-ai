package br.com.orcamentoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.orcamentoapi.dto.ResumoGastosResponse;
import br.com.orcamentoapi.dto.TransacaoResponse;
import br.com.orcamentoapi.exception.RegraNegocioException;
import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.model.Transacao;
import br.com.orcamentoapi.repository.TransacaoRepository;

class TransacaoServiceTest {

    private TransacaoRepository repository;
    private TransacaoService service;

    @BeforeEach
    void setUp() {
        repository = mock(TransacaoRepository.class);
        when(repository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new TransacaoService(repository);
    }

    @Test
    void registraGastoValido() {
        TransacaoResponse resposta = service.registrar("  Compras no mercado  ", new BigDecimal("80"), Categoria.MERCADO);

        assertThat(resposta.descricao()).isEqualTo("Compras no mercado");
        assertThat(resposta.valor()).isEqualByComparingTo("80.00");
        assertThat(resposta.categoria()).isEqualTo(Categoria.MERCADO);
        assertThat(resposta.criadaEm()).isNotNull();
    }

    @Test
    void arredondaValorParaCentavos() {
        TransacaoResponse resposta = service.registrar("Remédio", new BigDecimal("19.899"), Categoria.FARMACIA);

        assertThat(resposta.valor()).isEqualByComparingTo("19.90");
    }

    @Test
    void valorZeroOuNegativoNaoEhSalvo() {
        assertThatThrownBy(() -> service.registrar("Mercado", BigDecimal.ZERO, Categoria.MERCADO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> service.registrar("Mercado", new BigDecimal("-10"), Categoria.MERCADO))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void valorAcimaDoLimiteEhRejeitado() {
        assertThatThrownBy(() -> service.registrar("Carro", new BigDecimal("1000000.01"), Categoria.TRANSPORTE))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void descricaoVaziaEhRejeitada() {
        assertThatThrownBy(() -> service.registrar("   ", new BigDecimal("10"), Categoria.OUTROS))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Descrição");
    }

    @Test
    void categoriaNulaEhRejeitada() {
        assertThatThrownBy(() -> service.registrar("Cinema", new BigDecimal("40"), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void resumoSomaPorCategoriaDoMaiorParaOMenor() {
        when(repository.findAll()).thenReturn(List.of(
                new Transacao("Mercado", new BigDecimal("80.00"), Categoria.MERCADO),
                new Transacao("Padaria", new BigDecimal("15.50"), Categoria.MERCADO),
                new Transacao("Gasolina", new BigDecimal("200.00"), Categoria.TRANSPORTE),
                new Transacao("Remédio", new BigDecimal("19.90"), Categoria.FARMACIA)));

        ResumoGastosResponse resumo = service.resumo();

        assertThat(resumo.quantidade()).isEqualTo(4);
        assertThat(resumo.total()).isEqualByComparingTo("315.40");
        assertThat(resumo.porCategoria()).extracting("categoria")
                .containsExactly(Categoria.TRANSPORTE, Categoria.MERCADO, Categoria.FARMACIA);
        assertThat(resumo.porCategoria().get(1).quantidade()).isEqualTo(2);
        assertThat(resumo.porCategoria().get(1).total()).isEqualByComparingTo("95.50");
    }

    @Test
    void resumoSemGastosVemZerado() {
        when(repository.findAll()).thenReturn(List.of());

        ResumoGastosResponse resumo = service.resumo();

        assertThat(resumo.quantidade()).isZero();
        assertThat(resumo.total()).isEqualByComparingTo("0");
        assertThat(resumo.porCategoria()).isEmpty();
    }

    @Test
    void listarSemCategoriaTrazTodos() {
        service.listar(null);
        verify(repository).findAllByOrderByCriadaEmDesc();

        service.listar(Categoria.LAZER);
        verify(repository).findAllByCategoriaOrderByCriadaEmDesc(Categoria.LAZER);
    }
}
