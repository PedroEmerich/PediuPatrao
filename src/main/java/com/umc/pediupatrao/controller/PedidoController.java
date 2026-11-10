package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.OrderDtos;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/pedidos", "/api/v1/pedidos"})
public class PedidoController {

    public record DescontoRequest(BigDecimal percentual) {}
    public record CancelamentoRequest(String justificativa) {}

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(pedidoService.obterPorId(id));
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody PedidoRequest payload) {
        Pedido criado = pedidoService.criarPedido(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pedido> atualizarStatus(
            @PathVariable String id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) OrderDtos.UpdateStatusDto corpoStatus
    ) {
        String statusAlvo = status != null ? status : (corpoStatus != null ? corpoStatus.status() : null);
        if (statusAlvo == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(pedidoService.atualizarStatus(id, statusAlvo));
    }

    @PostMapping("/{id}/desconto")
    public ResponseEntity<Pedido> aplicarDesconto(
            @PathVariable String id,
            @RequestBody OrderDtos.ApplyDiscountDto dto
    ) {
        return ResponseEntity.ok(pedidoService.aplicarDesconto(id, dto.percentual()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Pedido> cancelarPedido(
            @PathVariable String id,
            @RequestBody(required = false) OrderDtos.CancelOrderDto dto,
            @RequestParam(required = false) String justificativa
    ) {
        String motivo = dto != null && dto.motivo() != null ? dto.motivo() : justificativa;
        return ResponseEntity.ok(pedidoService.cancelar(id, motivo));
    }
}
