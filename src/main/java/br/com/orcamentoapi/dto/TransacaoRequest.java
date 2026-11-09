package br.com.orcamentoapi.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import br.com.orcamentoapi.model.Categoria;

public record TransacaoRequest(
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @DecimalMax(value = "1000000.00", message = "Valor máximo é R$ 1.000.000,00")
        BigDecimal valor,

        @NotNull(message = "Categoria é obrigatória (veja GET /transacoes/categorias)")
        Categoria categoria) {
}
