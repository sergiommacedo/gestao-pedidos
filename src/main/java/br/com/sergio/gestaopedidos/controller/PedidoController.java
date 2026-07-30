package br.com.sergio.gestaopedidos.controller;

import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        return ResponseEntity.ok(
                pedidoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                pedidoService.buscarPorId(id)
        );
    }

    @PostMapping
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