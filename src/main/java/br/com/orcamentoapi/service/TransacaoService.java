package br.com.orcamentoapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.orcamentoapi.dto.CategoriaResponse;
import br.com.orcamentoapi.dto.ResumoGastosResponse;
import br.com.orcamentoapi.dto.TotalCategoriaResponse;
import br.com.orcamentoapi.dto.TransacaoRequest;
import br.com.orcamentoapi.dto.TransacaoResponse;
import br.com.orcamentoapi.exception.RegraNegocioException;
import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.model.Transacao;
import br.com.orcamentoapi.repository.TransacaoRepository;

/**
 * Regras de negócio das transações.
 *
 * É usado tanto pelo controller REST quanto pelas ferramentas da IA (FerramentasFinanceiras).
 * Por isso as validações ficam aqui e não só no DTO: quando a IA registra um gasto,
 * a requisição não passa pelo @Valid do controller.
 */
@Service
public class TransacaoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("1000000.00");
    private static final int TAMANHO_MAXIMO_DESCRICAO = 255;

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public TransacaoResponse registrar(TransacaoRequest request) {
        return registrar(request.descricao(), request.valor(), request.categoria());
    }

    public TransacaoResponse registrar(String descricao, BigDecimal valor, Categoria categoria) {
        String descricaoValidada = validarDescricao(descricao);
        BigDecimal valorValidado = validarValor(valor);
        if (categoria == null) {
            throw new RegraNegocioException("Categoria é obrigatória. Categorias aceitas: "
                    + Arrays.toString(Categoria.values()));
        }

        Transacao transacao = new Transacao(descricaoValidada, valorValidado, categoria);
        return TransacaoResponse.de(repository.save(transacao));
    }

    public List<TransacaoResponse> listar(Categoria categoria) {
        List<Transacao> transacoes = categoria == null
                ? repository.findAllByOrderByCriadaEmDesc()
                : repository.findAllByCategoriaOrderByCriadaEmDesc(categoria);

        return transacoes.stream()
                .map(TransacaoResponse::de)
                .toList();
    }

    /** Soma feita no código (e não pela IA), para o total ser sempre exato. */
    public ResumoGastosResponse resumo() {
        List<Transacao> transacoes = repository.findAll();

        Map<Categoria, BigDecimal> totais = new EnumMap<>(Categoria.class);
        Map<Categoria, Long> quantidades = new EnumMap<>(Categoria.class);
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            totais.merge(t.getCategoria(), t.getValor(), BigDecimal::add);
            quantidades.merge(t.getCategoria(), 1L, Long::sum);
            totalGeral = totalGeral.add(t.getValor());
        }

        List<TotalCategoriaResponse> porCategoria = totais.entrySet().stream()
                .map(e -> new TotalCategoriaResponse(e.getKey(), quantidades.get(e.getKey()),
                        e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(TotalCategoriaResponse::total).reversed())
                .toList();

        return new ResumoGastosResponse(transacoes.size(), totalGeral.setScale(2, RoundingMode.HALF_UP), porCategoria);
    }

    public List<CategoriaResponse> categorias() {
        return Arrays.stream(Categoria.values())
                .map(c -> new CategoriaResponse(c.name(), c.getDescricao()))
                .toList();
    }

    private String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("Descrição é obrigatória");
        }
        String limpa = descricao.trim();
        if (limpa.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new RegraNegocioException("Descrição deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
        return limpa;
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null) {
            throw new RegraNegocioException("Valor é obrigatório");
        }
        BigDecimal arredondado = valor.setScale(2, RoundingMode.HALF_UP);
        if (arredondado.signum() <= 0) {
            throw new RegraNegocioException("Valor deve ser maior que zero");
        }
        if (arredondado.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraNegocioException("Valor máximo é R$ 1.000.000,00");
        }
        return arredondado;
    }
}
