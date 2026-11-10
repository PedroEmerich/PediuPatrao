package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.*;

import com.umc.pediupatrao.dto.DashboardDtos.CategoriaVenda;
import com.umc.pediupatrao.dto.DashboardDtos.HoraVenda;
import com.umc.pediupatrao.dto.DashboardDtos.PontoVenda;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.Resumo;
import com.umc.pediupatrao.dto.DashboardDtos.StatusQuantidade;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardCalculadora.Agrupamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardCalculadoraTest {

    private static final Map<String, String> CATEGORIAS = Map.of(
            "pz1", "Pizza", "pz2", "Pizza", "bb1", "Bebida");

    private List<Pedido> pedidos;

    private static Pedido pedido(String data, int hora, StatusPedido status, BigDecimal desconto, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setDataHoraEntrada(LocalDate.parse(data).atTime(hora, 0));
        p.getItens().addAll(List.of(itens));
        p.setStatus(StatusPedido.RECEBIDO);
        p.recalcularValores();
        if (desconto != null) {
            p.aplicarDesconto(desconto);
        }
        p.setStatus(status);
        return p;
    }

    private static ItemPedido item(String id, String nome, int qtd, String preco) {
        return new ItemPedido(id, nome, qtd, new BigDecimal(preco));
    }

    @BeforeEach
    void montarMassa() {
        pedidos = List.of(
                // 01/11 às 19h: 2 pizzas calabresa (100) + 1 refri (10) = 110, finalizado
                pedido("2026-11-01", 19, StatusPedido.FINALIZADO, null,
                        item("pz1", "Calabresa", 2, "50.00"), item("bb1", "Refri", 1, "10.00")),
                // 01/11 às 20h: 1 quatro queijos (60) com 10% de desconto = 54, finalizado
                pedido("2026-11-01", 20, StatusPedido.FINALIZADO, new BigDecimal("10"),
                        item("pz2", "Quatro Queijos", 1, "60.00")),
                // 03/11 às 12h: 3 refris (30), em preparação
                pedido("2026-11-03", 12, StatusPedido.EM_PREPARACAO, null,
                        item("bb1", "Refri", 3, "10.00")),
                // 03/11 às 21h: cancelado (não pode entrar no faturamento)
                pedido("2026-11-03", 21, StatusPedido.CANCELADO, null,
                        item("pz1", "Calabresa", 5, "50.00"))
        );
    }

    @Test
    @DisplayName("RF01 - Resumo ignora cancelados no faturamento e calcula ticket médio")
    void resumoGeral() {
        Resumo r = new DashboardCalculadora(pedidos, CATEGORIAS, null).resumo(new BigDecimal("97.00"));

        assertEquals(new BigDecimal("194.00"), r.faturamento()); // 110 + 54 + 30
        assertEquals(4, r.totalPedidos());
        assertEquals(3, r.pedidosValidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(2, r.pedidosFinalizados());
        assertEquals(1, r.pedidosEmAndamento());
        assertEquals(new BigDecimal("64.67"), r.ticketMedio()); // 194 / 3
        assertEquals(new BigDecimal("25.00"), r.taxaCancelamento()); // 1 de 4
        assertEquals(new BigDecimal("6.00"), r.totalDescontos()); // 10% de 60
        assertEquals(7, r.itensVendidos()); // 2 + 1 + 1 + 3
        assertEquals(new BigDecimal("100.00"), r.variacaoFaturamento()); // 194 vs 97 = +100%
    }

    @Test
    @DisplayName("RF01 - Sem pedidos, indicadores ficam zerados (sem divisão por zero)")
    void resumoVazio() {
        Resumo r = new DashboardCalculadora(List.of(), CATEGORIAS, null).resumo(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), r.faturamento());
        assertEquals(new BigDecimal("0.00"), r.ticketMedio());
        assertEquals(new BigDecimal("0.00"), r.taxaCancelamento());
        assertNull(r.variacaoFaturamento());
    }

    @Test
    @DisplayName("RF02 - Série diária preenche dias sem venda com zero")
    void vendasPorDia() {
        List<PontoVenda> serie = new DashboardCalculadora(pedidos, CATEGORIAS, null)
                .vendasPorPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), Agrupamento.DIA);

        assertEquals(3, serie.size());
        assertEquals(new PontoVenda("2026-11-01", new BigDecimal("164.00"), 2), serie.get(0));
        assertEquals(new PontoVenda("2026-11-02", new BigDecimal("0.00"), 0), serie.get(1));
        assertEquals(new PontoVenda("2026-11-03", new BigDecimal("30.00"), 1), serie.get(2));
    }

    @Test
    @DisplayName("RF02 - Agrupamento mensal soma o período inteiro")
    void vendasPorMes() {
        List<PontoVenda> serie = new DashboardCalculadora(pedidos, CATEGORIAS, null)
                .vendasPorPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), Agrupamento.MES);
        assertEquals(1, serie.size());
        assertEquals("2026-11", serie.get(0).periodo());
        assertEquals(new BigDecimal("194.00"), serie.get(0).faturamento());
    }

    @Test
    @DisplayName("RF02 - Vendas por categoria aplicam o desconto proporcional")
    void vendasPorCategoria() {
        List<CategoriaVenda> cats = new DashboardCalculadora(pedidos, CATEGORIAS, null).vendasPorCategoria();
        assertEquals(2, cats.size());
        assertEquals(new CategoriaVenda("Pizza", 3, new BigDecimal("154.00")), cats.get(0)); // 100 + 54
        assertEquals(new CategoriaVenda("Bebida", 4, new BigDecimal("40.00")), cats.get(1));
    }

    @Test
    @DisplayName("RF02 - Distribuição por hora tem sempre 24 posições")
    void vendasPorHora() {
        List<HoraVenda> horas = new DashboardCalculadora(pedidos, CATEGORIAS, null).vendasPorHora();
        assertEquals(24, horas.size());
        assertEquals(1, horas.get(19).pedidos());
        assertEquals(1, horas.get(12).pedidos());
        assertEquals(0, horas.get(21).pedidos()); // o pedido das 21h foi cancelado
    }

    @Test
    @DisplayName("RF03 - Situação dos pedidos lista todos os status com percentual")
    void situacao() {
        List<StatusQuantidade> st = new DashboardCalculadora(pedidos, CATEGORIAS, null).situacaoPedidos();
        assertEquals(StatusPedido.values().length, st.size());
        StatusQuantidade fin = st.stream().filter(s -> s.status().equals("FINALIZADO")).findFirst().orElseThrow();
        assertEquals(2, fin.quantidade());
        assertEquals(new BigDecimal("50.00"), fin.percentual());
        StatusQuantidade pronto = st.stream().filter(s -> s.status().equals("PRONTO")).findFirst().orElseThrow();
        assertEquals(0, pronto.quantidade());
    }

    @Test
    @DisplayName("RF04 - Ranking por quantidade e por faturamento")
    void ranking() {
        DashboardCalculadora calc = new DashboardCalculadora(pedidos, CATEGORIAS, null);

        List<ProdutoRanking> porQtd = calc.topProdutos(5, "quantidade");
        assertEquals("Refri", porQtd.get(0).nome());
        assertEquals(4, porQtd.get(0).quantidade());
        assertEquals(1, porQtd.get(0).posicao());

        List<ProdutoRanking> porValor = calc.topProdutos(1, "faturamento");
        assertEquals(1, porValor.size());
        assertEquals("Calabresa", porValor.get(0).nome()); // 100 (o pedido cancelado não conta)
        assertEquals(new BigDecimal("100.00"), porValor.get(0).faturamento());
    }

    @Test
    @DisplayName("RF05 - Filtro de categoria considera apenas os itens daquela categoria")
    void filtroCategoria() {
        DashboardCalculadora calc = new DashboardCalculadora(pedidos, CATEGORIAS, "bebida");
        Resumo r = calc.resumo(null);
        assertEquals(new BigDecimal("40.00"), r.faturamento()); // 10 + 30
        assertEquals(2, r.totalPedidos()); // pedidos sem bebida saem da conta
        assertEquals(4, r.itensVendidos());
    }

    @Test
    @DisplayName("RF07 - Produto sem categoria cadastrada é agrupado em 'Outros'; pedidos sem data são descartados")
    void tratamentoDeDados() {
        Pedido semData = pedido("2026-11-01", 10, StatusPedido.FINALIZADO, null, item("pz1", "Calabresa", 1, "50.00"));
        semData.setDataHoraEntrada(null);
        Pedido produtoRemovido = pedido("2026-11-02", 10, StatusPedido.FINALIZADO, null, item("xx9", "Produto antigo", 2, "5.00"));

        DashboardCalculadora calc = new DashboardCalculadora(List.of(semData, produtoRemovido), CATEGORIAS, null);
        assertEquals(1, calc.resumo(null).totalPedidos());
        assertEquals(DashboardCalculadora.SEM_CATEGORIA, calc.vendasPorCategoria().get(0).categoria());
    }

    @Test
    @DisplayName("Agrupamento inválido gera erro de validação")
    void agrupamentoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Agrupamento.de("ano"));
        assertEquals(Agrupamento.SEMANA, Agrupamento.de("Semana"));
        assertEquals(Agrupamento.DIA, Agrupamento.de(null));
    }

    @Test
    @DisplayName("Variação percentual sem base anterior retorna nulo")
    void variacao() {
        assertNull(DashboardCalculadora.variacao(BigDecimal.TEN, BigDecimal.ZERO));
        assertEquals(new BigDecimal("-50.00"), DashboardCalculadora.variacao(new BigDecimal("50"), new BigDecimal("100")));
    }
}
