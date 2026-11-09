package br.com.orcamentoapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemRequest(
        @NotBlank(message = "Mensagem é obrigatória")
        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem) {
}
