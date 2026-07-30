package br.com.sergio.gestaopedidos.controller;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioRequest;
import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.exception.ApiError;
import br.com.sergio.gestaopedidos.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(
        name = "Usuários",
        description = "Operações para gerenciamento de usuários"
)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(
            summary = "Listar usuários",
            description = "Retorna todos os usuários cadastrados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuários listados com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = UsuarioResponse.class
                            )
                    )
            )
    )
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(
                usuarioService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar usuário por ID",
            description = "Retorna um usuário através do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UsuarioResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(
                    description = "Identificador do usuário",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                usuarioService.buscarPorId(id)
        );
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar usuário",
            description = "Cadastra um novo usuário no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário cadastrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UsuarioResponse.class
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
    public ResponseEntity<UsuarioResponse> salvar(
            @Valid @RequestBody UsuarioRequest request
    ) {
        UsuarioResponse usuarioSalvo =
                usuarioService.salvar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioSalvo);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar usuário",
            description = "Atualiza os dados de um usuário existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UsuarioResponse.class
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
                    description = "Usuário não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiError.class
                            )
                    )
            )
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(
                    description = "Identificador do usuário",
                    example = "1"
            )
            @PathVariable Long id,

            @Valid @RequestBody UsuarioRequest request
    ) {
        return ResponseEntity.ok(
                usuarioService.atualizar(id, request)
        );
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar usuário",
            description = "Altera o status do usuário para ativo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuário ativado com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
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
                    description = "Identificador do usuário",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        usuarioService.ativar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    @Operation(
            summary = "Inativar usuário",
            description = "Altera o status do usuário para inativo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuário inativado com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
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
                    description = "Identificador do usuário",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        usuarioService.inativar(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir usuário",
            description = "Exclui permanentemente um usuário cadastrado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuário excluído com sucesso",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado",
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
                    description = "Identificador do usuário",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        usuarioService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}