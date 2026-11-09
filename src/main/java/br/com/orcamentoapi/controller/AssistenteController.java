package br.com.orcamentoapi.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.orcamentoapi.dto.MensagemRequest;
import br.com.orcamentoapi.dto.MensagemResponse;
import br.com.orcamentoapi.exception.AudioInvalidoException;
import br.com.orcamentoapi.ia.AssistenteFinanceiro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/assistente")
@Tag(name = "Assistente", description = "Comandos em linguagem natural, por voz ou texto")
public class AssistenteController {

    private static final MediaType AUDIO_MP3 = MediaType.parseMediaType("audio/mpeg");

    private final AssistenteFinanceiro assistente;

    public AssistenteController(AssistenteFinanceiro assistente) {
        this.assistente = assistente;
    }

    @PostMapping(value = "/voz", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia um áudio com o comando e recebe a resposta em MP3")
    public ResponseEntity<Resource> voz(@RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new AudioInvalidoException("Envie um arquivo de áudio no campo 'arquivo'");
        }

        String mensagem = assistente.transcrever(arquivo.getResource());
        String resposta = assistente.responder(mensagem);
        byte[] audio = assistente.falar(resposta);

        return ResponseEntity.ok()
                .contentType(AUDIO_MP3)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resposta.mp3").build().toString())
                // O que a IA entendeu do áudio (codificado, porque cabeçalhos HTTP não aceitam acentos)
                .header("X-Transcricao", URLEncoder.encode(mensagem, StandardCharsets.UTF_8))
                .body(new ByteArrayResource(audio));
    }

    @PostMapping("/texto")
    @Operation(summary = "Mesmo fluxo da IA, mas em texto: mais fácil de testar e mais barato")
    public MensagemResponse texto(@RequestBody @Valid MensagemRequest request) {
        return new MensagemResponse(request.mensagem(), assistente.responder(request.mensagem()));
    }
}
