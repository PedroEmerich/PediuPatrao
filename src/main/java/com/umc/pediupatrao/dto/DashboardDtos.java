package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objetos de resposta dos endpoints do Dashboard Administrativo.
 * Todos os valores monetários são em reais (BigDecimal com 2 casas).
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Filtros aplicados na consulta (ecoados na resposta para conferência). */
    public record FiltroAplicado(
            LocalDate dataInicio,
            LocalDate dataFim,
            String status,
            String categoria,
            String clienteId
    ) {}

    /** RF01 — Indicadores gerais de vendas. */
    public record Resumo(
            BigDecimal faturamento,
            long totalPedidos,
            long pedidosValidos,
            long pedidosCancelados,
            long pedidosEmAndamento,
            long pedidosFinalizados,
            BigDecimal ticketMedio,
            BigDecimal taxaCancelamento,
            BigDecimal totalDescontos,
            long itensVendidos,
            BigDecimal variacaoFaturamento
    ) {}

    /** RF02 — Ponto da série temporal de vendas. */
    public record PontoVenda(
            String periodo,
            BigDecimal faturamento,
            long pedidos
    ) {}

    /** RF03 — Quantidade de pedidos por situação. */
    public record StatusQuantidade(
            String status,
            long quantidade,
            BigDecimal percentual
    ) {}

    /** RF04 — Ranking de produtos. */
    public record ProdutoRanking(
            int posicao,
            String produtoId,
            String nome,
            String categoria,
            long quantidade,
            BigDecimal faturamento
    ) {}

    /** RF02 — Vendas por categoria de produto. */
    public record CategoriaVenda(
            String categoria,
            long quantidade,
            BigDecimal faturamento
    ) {}

    /** RF02 — Distribuição de pedidos por hora do dia (horário de pico). */
    public record HoraVenda(
            int hora,
            long pedidos,
            BigDecimal faturamento
    ) {}

    /** RF05 — Opções disponíveis para os filtros da tela. */
    public record OpcoesFiltro(
            List<String> status,
            List<String> categorias,
            List<ClienteOpcao> clientes,
            LocalDate primeiroPedido,
            LocalDate ultimoPedido
    ) {}

    public record ClienteOpcao(String id, String nome) {}

    /** Resposta consolidada usada pela tela (uma única chamada). */
    public record Painel(
            FiltroAplicado filtro,
            LocalDateTime geradoEm,
            Resumo resumo,
            List<PontoVenda> vendas,
            List<StatusQuantidade> status,
            List<ProdutoRanking> topProdutos,
            List<CategoriaVenda> categorias,
            List<HoraVenda> horarios
    ) {}

    /** Formato padrão de erro da API. */
    public record ErroApi(
            LocalDateTime timestamp,
            int status,
            String erro,
            String mensagem,
            String caminho
    ) {}
}
