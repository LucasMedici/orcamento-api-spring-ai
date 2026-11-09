package br.com.orcamentoapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.orcamentoapi.dto.CategoriaResponse;
import br.com.orcamentoapi.dto.ResumoGastosResponse;
import br.com.orcamentoapi.dto.TransacaoRequest;
import br.com.orcamentoapi.dto.TransacaoResponse;
import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transacoes")
@Tag(name = "Transações", description = "Cadastro, listagem e resumo de gastos")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um gasto")
    public TransacaoResponse registrar(@RequestBody @Valid TransacaoRequest request) {
        return transacaoService.registrar(request);
    }

    @GetMapping
    @Operation(summary = "Lista os gastos, dos mais recentes para os mais antigos (filtro opcional por categoria)")
    public List<TransacaoResponse> listar(@RequestParam(required = false) Categoria categoria) {
        return transacaoService.listar(categoria);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Total gasto e total por categoria, da maior para a menor")
    public ResumoGastosResponse resumo() {
        return transacaoService.resumo();
    }

    @GetMapping("/categorias")
    @Operation(summary = "Lista as categorias disponíveis")
    public List<CategoriaResponse> categorias() {
        return transacaoService.categorias();
    }
}
