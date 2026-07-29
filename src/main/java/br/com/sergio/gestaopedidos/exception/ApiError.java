package br.com.sergio.gestaopedidos.exception;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiError(
        LocalDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path
) {
}