package br.com.sergio.gestaopedidos.dto.cliente;

import lombok.Builder;

@Builder
public record ClienteResponse(

        Long id,
        String nome,
        String telefone,
        String endereco,
        String numero,
        String bairro,
        String cidade,
        String cep,
        String complemento

) {
}