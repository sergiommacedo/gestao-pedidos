package br.com.sergio.gestaopedidos.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta padrão para erros ocorridos na API")
public record ApiError(

        @Schema(
                description = "Data e hora em que o erro ocorreu",
                example = "2026-07-30T11:30:00"
        )
        LocalDateTime timestamp,

        @Schema(
                description = "Código HTTP do erro",
                example = "404"
        )
        Integer status,

        @Schema(
                description = "Descrição correspondente ao status HTTP",
                example = "Not Found"
        )
        String error,

        @Schema(
                description = "Mensagem detalhada sobre o erro",
                example = "Cliente não encontrado."
        )
        String message,

        @Schema(
                description = "Endpoint em que o erro ocorreu",
                example = "/api/clientes/10"
        )
        String path

) {
}