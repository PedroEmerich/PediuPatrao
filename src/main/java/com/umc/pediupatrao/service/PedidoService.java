package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.OrderDtos;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            AuditoriaService auditoriaService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHoraEntrada"));
    }

    public Pedido obterPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado com identificador: " + id));
    }

    public Pedido criarPedido(PedidoRequest req) {
        if (req == null || req.clienteId() == null || req.itens() == null || req.itens().isEmpty()) {
            throw new IllegalArgumentException("Dados de pedido incompletos");
        }

        Cliente cliente = clienteRepository.findById(req.clienteId())
                .orElseThrow(() -> new NoSuchElementException("Cliente inválido: " + req.clienteId()));

        Pedido novo = new Pedido();
        novo.setClienteId(cliente.getId());
        novo.setClienteNome(cliente.getNome());
        novo.setStatus(StatusPedido.RECEBIDO);
        novo.setResponsavelEntrada(auditoriaService.usuarioAtual());
        novo.setDataHoraEntrada(LocalDateTime.now());

        List<ItemPedido> itensMontados = new ArrayList<>();
        for (PedidoRequest.Item item : req.itens()) {
            Produto prod = produtoRepository.findById(item.produtoId())
                    .orElseThrow(() -> new NoSuchElementException("Produto inexistente: " + item.produtoId()));
            itensMontados.add(new ItemPedido(
                    prod.getId(),
                    prod.getNome(),
                    item.quantidade(),
                    BigDecimal.valueOf(prod.getPreco())
            ));
        }
        novo.setItens(itensMontados);
        novo.recalcularValores();

        Pedido persistido = pedidoRepository.save(novo);

        auditoriaService.registrar(
                "CRIACAO_PEDIDO",
                "Pedido",
                persistido.getId(),
                List.of(
                        Alteracao.de("status", null, persistido.getStatus()),
                        Alteracao.de("total", null, persistido.getTotal())
                ),
                null
        );

        return persistido;
    }

    public Pedido criarComDto(OrderDtos.CreateOrderDto dto) {
        List<PedidoRequest.Item> items = new ArrayList<>();
        if (dto.itens() != null) {
            for (OrderDtos.CreateOrderItemDto item : dto.itens()) {
                items.add(new PedidoRequest.Item(item.produtoId(), item.quantidade()));
            }
        }
        return criarPedido(new PedidoRequest(dto.clienteId(), items));
    }

    public Pedido atualizarStatus(String id, String novoStatusNome) {
        Pedido pedido = obterPorId(id);
        StatusPedido anterior = pedido.getStatus();
        StatusPedido proximo = StatusPedido.valueOf(novoStatusNome.trim().toUpperCase());

        pedido.evoluirStatus(proximo, auditoriaService.usuarioAtual());
        Pedido atualizado = pedidoRepository.save(pedido);

        auditoriaService.registrar(
                "ALTERACAO_STATUS",
                "Pedido",
                id,
                List.of(Alteracao.de("status", anterior, atualizado.getStatus())),
                null
        );

        return atualizado;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal taxaPercentual) {
        Pedido pedido = obterPorId(id);
        BigDecimal descontoAnterior = pedido.getPercentualDesconto();
        BigDecimal totalAnterior = pedido.getTotal();

        pedido.aplicarDesconto(taxaPercentual);
        Pedido atualizado = pedidoRepository.save(pedido);

        auditoriaService.registrar(
                "DESCONTO",
                "Pedido",
                id,
                List.of(
                        Alteracao.de("percentualDesconto", descontoAnterior, atualizado.getPercentualDesconto()),
                        Alteracao.de("total", totalAnterior, atualizado.getTotal())
                ),
                null
        );

        return atualizado;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String motivo) {
        Pedido pedido = obterPorId(id);
        StatusPedido anterior = pedido.getStatus();

        pedido.cancelar(motivo, auditoriaService.usuarioAtual());
        Pedido atualizado = pedidoRepository.save(pedido);

        auditoriaService.registrar(
                "CANCELAMENTO",
                "Pedido",
                id,
                List.of(Alteracao.de("status", anterior, atualizado.getStatus())),
                motivo
        );

        return atualizado;
    }
}
