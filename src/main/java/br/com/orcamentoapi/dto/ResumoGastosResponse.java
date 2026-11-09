package br.com.orcamentoapi.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoGastosResponse(
        long quantidade,
        BigDecimal total,
        List<TotalCategoriaResponse> porCategoria) {
}
