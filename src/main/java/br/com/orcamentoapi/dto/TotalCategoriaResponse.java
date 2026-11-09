package br.com.orcamentoapi.dto;

import java.math.BigDecimal;

import br.com.orcamentoapi.model.Categoria;

public record TotalCategoriaResponse(
        Categoria categoria,
        long quantidade,
        BigDecimal total) {
}
