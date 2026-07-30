package br.com.sergio.gestaopedidos.controller;

import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.exception.ApiError;
import br.com.sergio.gestaopedidos.service.PedidoService;
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
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(
        name = "Pedidos",
        description = "Operações para gerenciamento de pedidos"
)
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    @Operation(
            summary = "Listar pedidos",
            description = "Retorna todos os pedidos cadastrados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pedidos listados com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = PedidoResponse.class
                            )
                    )
            )
    )
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        return ResponseEntity.ok(
                pedidoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar pedido por ID",
            description = "Retorna um pedido através do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pedido encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PedidoResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<PedidoResponse> buscarPorId(
            @Parameter(
                    description = "Identificador do pedido",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                pedidoService.buscarPorId(id)
        );
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar pedido",
            description = "Cadastra um novo pedido no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Pedido cadastrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PedidoResponse.class
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
                    description = "Cliente ou produto não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<PedidoResponse> salvar(
            @Valid @RequestBody PedidoRequest request
    ) {
        PedidoResponse pedidoSalvo =
                pedidoService.salvar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedidoSalvo);
    }
}