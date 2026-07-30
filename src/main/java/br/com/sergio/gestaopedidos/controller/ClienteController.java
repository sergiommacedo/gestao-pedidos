package br.com.sergio.gestaopedidos.controller;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.exception.ApiError;
import br.com.sergio.gestaopedidos.service.ClienteService;
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
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(
        name = "Clientes",
        description = "Operações para gerenciamento de clientes"
)
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(
            summary = "Listar clientes",
            description = "Retorna todos os clientes cadastrados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Clientes listados com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = ClienteResponse.class
                            )
                    )
            )
    )
    public ResponseEntity<List<ClienteResponse>> listarTodos() {
        return ResponseEntity.ok(
                clienteService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar cliente por ID",
            description = "Retorna um cliente através do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ClienteResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<ClienteResponse> buscarPorId(
            @Parameter(
                    description = "Identificador do cliente",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                clienteService.buscarPorId(id)
        );
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar cliente",
            description = "Cadastra um novo cliente no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cliente cadastrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ClienteResponse.class
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
    public ResponseEntity<ClienteResponse> salvar(
            @Valid @RequestBody ClienteRequest request
    ) {
        ClienteResponse response =
                clienteService.salvar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar cliente",
            description = "Atualiza os dados de um cliente existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ClienteResponse.class
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
                    description = "Cliente não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<ClienteResponse> atualizar(
            @Parameter(
                    description = "Identificador do cliente",
                    example = "1"
            )
            @PathVariable Long id,

            @Valid @RequestBody ClienteRequest request
    ) {
        return ResponseEntity.ok(
                clienteService.atualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir cliente",
            description = "Exclui permanentemente um cliente cadastrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Cliente excluído com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente não encontrado",
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
                    description = "Identificador do cliente",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        clienteService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}