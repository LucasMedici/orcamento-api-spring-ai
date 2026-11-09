package br.com.orcamentoapi.ia;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import br.com.orcamentoapi.exception.AudioInvalidoException;

/**
 * Esconde os três modelos de IA usados no fluxo de voz atrás de métodos simples
 * (funciona como uma Facade): transcrever, responder e falar.
 * O controller não precisa saber que existem modelos diferentes para cada etapa.
 */
@Component
public class AssistenteFinanceiro {

    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;

    public AssistenteFinanceiro(TranscriptionModel transcriptionModel,
                                ChatClient chatClient,
                                TextToSpeechModel textToSpeechModel) {
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClient;
        this.textToSpeechModel = textToSpeechModel;
    }

    /** Áudio → texto (Whisper). */
    public String transcrever(Resource audio) {
        String texto = transcriptionModel.transcribe(audio);
        if (texto == null || texto.isBlank()) {
            throw new AudioInvalidoException("Não foi possível entender o áudio. Tente gravar novamente");
        }
        return texto.trim();
    }

    /** Texto → resposta da IA, que pode chamar as ferramentas financeiras. */
    public String responder(String mensagem) {
        return chatClient.prompt()
                .user(mensagem)
                .call()
                .content();
    }

    /** Texto → áudio MP3. */
    public byte[] falar(String texto) {
        return textToSpeechModel.call(texto);
    }
}
