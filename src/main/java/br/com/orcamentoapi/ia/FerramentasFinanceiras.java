package br.com.orcamentoapi.ia;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import br.com.orcamentoapi.dto.ResumoGastosResponse;
import br.com.orcamentoapi.dto.TransacaoResponse;
import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.service.TransacaoService;

/**
 * TOOL CALLING
 *
 * Cada método com @Tool vira uma "ferramenta" que o modelo de linguagem pode chamar.
 * A IA lê o nome e a descrição, decide qual usar e preenche os parâmetros a partir da fala.
 * As ferramentas não acessam o banco direto: só delegam para o TransacaoService,
 * então as mesmas regras de negócio valem para a API REST e para a IA.
 */
@Component
public class FerramentasFinanceiras {

    private final TransacaoService transacaoService;

    public FerramentasFinanceiras(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @Tool(name = "registrar-gasto",
            description = "Registra um novo gasto. Use somente quando a pessoa informar o valor do gasto.")
    public TransacaoResponse registrarGasto(
            @ToolParam(description = "Descrição curta do gasto, por exemplo 'Compras no mercado'") String descricao,
            @ToolParam(description = "Valor em reais, com ponto decimal. Exemplo: 19 reais e 90 centavos = 19.90") BigDecimal valor,
            @ToolParam(description = "Categoria do gasto") Categoria categoria) {
        return transacaoService.registrar(descricao, valor, categoria);
    }

    @Tool(name = "listar-gastos-por-categoria",
            description = "Lista os gastos de uma categoria, dos mais recentes para os mais antigos. Valores em reais.")
    public List<TransacaoResponse> listarGastosPorCategoria(
            @ToolParam(description = "Categoria dos gastos") Categoria categoria) {
        return transacaoService.listar(categoria);
    }

    @Tool(name = "resumo-de-gastos",
            description = "Retorna o total gasto, a quantidade de gastos e o total por categoria (da maior para a menor). "
                    + "Use para perguntas como 'quanto eu gastei?' ou 'onde gastei mais?'. Valores em reais.")
    public ResumoGastosResponse resumoDeGastos() {
        return transacaoService.resumo();
    }
}
