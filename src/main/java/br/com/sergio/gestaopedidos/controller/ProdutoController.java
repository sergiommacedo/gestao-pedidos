package br.com.sergio.gestaopedidos.controller;

import br.com.sergio.gestaopedidos.dto.produto.ProdutoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.exception.ApiError;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@Tag(
        name = "Produtos",
        description = "Operações para gerenciamento de produtos"
)
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    @Operation(
            summary = "Listar produtos",
            description = "Retorna todos os produtos cadastrados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Produtos listados com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = ProdutoResponse.class
                            )
                    )
            )
    )
    public ResponseEntity<List<ProdutoResponse>> listarTodos() {
        return ResponseEntity.ok(
                produtoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar produto por ID",
            description = "Retorna um produto através do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ProdutoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<ProdutoResponse> buscarPorId(
            @Parameter(
                    description = "Identificador do produto",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                produtoService.buscarPorId(id)
        );
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar produto",
            description = "Cadastra um novo produto no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Produto cadastrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ProdutoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou regra de negócio violada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<ProdutoResponse> salvar(
            @Valid @RequestBody ProdutoRequest request
    ) {
        ProdutoResponse produtoSalvo =
                produtoService.salvar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(produtoSalvo);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar produto",
            description = "Atualiza os dados de um produto existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ProdutoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou regra de negócio violada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<ProdutoResponse> atualizar(
            @Parameter(
                    description = "Identificador do produto",
                    example = "1"
            )
            @PathVariable Long id,

            @Valid @RequestBody ProdutoRequest request
    ) {
        return ResponseEntity.ok(
                produtoService.atualizar(id, request)
        );
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar produto",
            description = "Altera o status do produto para ativo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Produto ativado com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<Void> ativar(
            @Parameter(
                    description = "Identificador do produto",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        produtoService.ativar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    @Operation(
            summary = "Inativar produto",
            description = "Altera o status do produto para inativo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Produto inativado com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<Void> inativar(
            @Parameter(
                    description = "Identificador do produto",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        produtoService.inativar(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir produto",
            description = "Exclui permanentemente um produto cadastrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Produto excluído com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<Void> excluir(
            @Parameter(
                    description = "Identificador do produto",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        produtoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}