package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardDtos.ErroApi;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Padroniza as respostas de erro da API REST (formato {@link ErroApi}).
 * Aplica-se somente aos controllers REST — as telas Thymeleaf continuam usando error.html.
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErroApi> requisicaoInvalida(Exception e, HttpServletRequest req) {
        String msg = e instanceof MethodArgumentTypeMismatchException m
                ? "Parâmetro '" + m.getName() + "' com valor inválido: " + m.getValue()
                : e instanceof HttpMessageNotReadableException ? "Corpo da requisição inválido ou mal formatado"
                : e.getMessage();
        return montar(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroApi> naoEncontrado(NoSuchElementException e, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage() != null ? e.getMessage() : "Recurso não encontrado", req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroApi> conflito(IllegalStateException e, HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApi> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        return montar(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> erroInterno(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição", req);
    }

    private ResponseEntity<ErroApi> montar(HttpStatus status, String mensagem, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(new ErroApi(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI()));
    }
}
