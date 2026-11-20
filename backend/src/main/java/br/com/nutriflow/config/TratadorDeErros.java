package br.com.nutriflow.config;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Converte os erros em respostas JSON no formato { "message": "..." }, que o frontend exibe. */
@RestControllerAdvice
public class TratadorDeErros {

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("message", mensagem));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> regra(ResponseStatusException e) {
        return resposta(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason() == null ? "Erro" : e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).distinct().collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Dados inválidos" : mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos: confira os campos preenchidos");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> conflito(Exception e) {
        return resposta(HttpStatus.CONFLICT, "A operação conflitou com outra feita ao mesmo tempo. Atualize a tela e tente de novo.");
    }
}
