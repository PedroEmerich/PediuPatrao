package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderDtos {

    public record CreateOrderItemDto(
            String produtoId,
            Integer quantidade
    ) {}

    public record CreateOrderDto(
            String clienteId,
            List<CreateOrderItemDto> itens
    ) {}

    public record UpdateStatusDto(
            String status
    ) {}

    public record ApplyDiscountDto(
            BigDecimal percentual
    ) {}

    public record CancelOrderDto(
            String motivo
    ) {}
}
