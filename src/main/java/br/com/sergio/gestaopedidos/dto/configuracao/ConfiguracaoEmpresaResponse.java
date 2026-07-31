package br.com.sergio.gestaopedidos.dto.configuracao;

import br.com.sergio.gestaopedidos.enums.TemaSistema;

public record ConfiguracaoEmpresaResponse(
        Long id,
        String nomeEmpresa,
        String nomeCurto,
        TemaSistema tema,
        String temaCss,
        String textoBoasVindas,
        String logoArquivo,
        String logoUrl
) {
}
