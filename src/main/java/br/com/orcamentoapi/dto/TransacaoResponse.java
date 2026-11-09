package br.com.orcamentoapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.model.Transacao;

public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        Categoria categoria,
        LocalDateTime criadaEm) {

    public static TransacaoResponse de(Transacao t) {
        return new TransacaoResponse(t.getId(), t.getDescricao(), t.getValor(), t.getCategoria(), t.getCriadaEm());
    }
}
