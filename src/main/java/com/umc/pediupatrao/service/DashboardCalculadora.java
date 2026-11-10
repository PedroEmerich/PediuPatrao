package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDtos.CategoriaVenda;
import com.umc.pediupatrao.dto.DashboardDtos.HoraVenda;
import com.umc.pediupatrao.dto.DashboardDtos.PontoVenda;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.Resumo;
import com.umc.pediupatrao.dto.DashboardDtos.StatusQuantidade;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Regras de cálculo dos indicadores do dashboard.
 *
 * Classe sem dependência de banco de dados para que as regras de negócio
 * possam ser testadas isoladamente (ver DashboardCalculadoraTest).
 *
 * Regras de negócio adotadas:
 * - Pedidos CANCELADOS não entram no faturamento, ticket médio, ranking nem gráficos de venda;
 *   aparecem apenas na situação dos pedidos e na taxa de cancelamento.
 * - O valor de cada item considera o desconto do pedido (proporcional).
 * - Quando há filtro de categoria, só os itens daquela categoria são somados.
 */
public class DashboardCalculadora {

    public static final String SEM_CATEGORIA = "Outros";
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final Set<StatusPedido> CONCLUIDOS = EnumSet.of(StatusPedido.FINALIZADO);

    public enum Agrupamento {
        DIA, SEMANA, MES;

        public static Agrupamento de(String valor) {
            if (valor == null || valor.isBlank()) {
                return DIA;
            }
            try {
                return valueOf(valor.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Agrupamento inválido: '" + valor + "'. Use dia, semana ou mes");
            }
        }
    }

    private final List<Pedido> pedidos;
    private final Map<String, String> categoriaPorProduto;
    private final String categoriaFiltro;

    public DashboardCalculadora(List<Pedido> pedidos, Map<String, String> categoriaPorProduto, String categoriaFiltro) {
        this.categoriaPorProduto = categoriaPorProduto != null ? categoriaPorProduto : Map.of();
        this.categoriaFiltro = categoriaFiltro;
        this.pedidos = (pedidos != null ? pedidos : List.<Pedido>of()).stream()
                .filter(p -> p.getDataHoraEntrada() != null)
                .filter(p -> categoriaFiltro == null || !itensConsiderados(p).isEmpty())
                .toList();
    }

    // ------------------------------------------------------------------ RF01

    public Resumo resumo(BigDecimal faturamentoPeriodoAnterior) {
        long total = pedidos.size();
        long cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();
        long finalizados = pedidos.stream().filter(p -> CONCLUIDOS.contains(p.getStatus())).count();
        long validos = total - cancelados;
        long emAndamento = validos - finalizados;

        BigDecimal faturamento = faturamento();
        BigDecimal ticket = validos == 0 ? zero() : faturamento.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP);
        BigDecimal taxaCancel = total == 0 ? zero()
                : BigDecimal.valueOf(cancelados).multiply(CEM).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        BigDecimal descontos = validos().stream()
                .map(p -> valorBruto(p).subtract(valorLiquido(p)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long itens = validos().stream()
                .flatMap(p -> itensConsiderados(p).stream())
                .mapToLong(ItemPedido::getQuantidade)
                .sum();

        return new Resumo(faturamento, total, validos, cancelados, emAndamento, finalizados,
                ticket, taxaCancel, descontos, itens, variacao(faturamento, faturamentoPeriodoAnterior));
    }

    public BigDecimal faturamento() {
        return validos().stream()
                .map(this::valorLiquido)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ RF02

    public List<PontoVenda> vendasPorPeriodo(LocalDate inicio, LocalDate fim, Agrupamento agrupamento) {
        Function<LocalDate, String> chave = switch (agrupamento) {
            case DIA -> d -> d.toString();
            case SEMANA -> d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
            case MES -> d -> d.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        };

        // Pré-preenche todos os períodos com zero para o gráfico não "pular" dias sem venda.
        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            mapa.putIfAbsent(chave.apply(d), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Pedido p : validos()) {
            String k = chave.apply(p.getDataHoraEntrada().toLocalDate());
            BigDecimal[] acc = mapa.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(valorLiquido(p));
            acc[1] = acc[1].add(BigDecimal.ONE);
        }

        List<PontoVenda> lista = new ArrayList<>();
        mapa.forEach((k, v) -> lista.add(new PontoVenda(k, v[0].setScale(2, RoundingMode.HALF_UP), v[1].longValue())));
        return lista;
    }

    public List<CategoriaVenda> vendasPorCategoria() {
        Map<String, BigDecimal[]> mapa = new TreeMap<>();
        for (Pedido p : validos()) {
            BigDecimal fator = fatorDesconto(p);
            for (ItemPedido i : itensConsiderados(p)) {
                BigDecimal[] acc = mapa.computeIfAbsent(categoria(i), x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                acc[0] = acc[0].add(BigDecimal.valueOf(i.getQuantidade()));
                acc[1] = acc[1].add(i.calcularSubtotal().multiply(fator));
            }
        }
        return mapa.entrySet().stream()
                .map(e -> new CategoriaVenda(e.getKey(), e.getValue()[0].longValue(),
                        e.getValue()[1].setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(CategoriaVenda::faturamento).reversed())
                .toList();
    }

    public List<HoraVenda> vendasPorHora() {
        long[] qtd = new long[24];
        BigDecimal[] valor = new BigDecimal[24];
        java.util.Arrays.fill(valor, BigDecimal.ZERO);
        for (Pedido p : validos()) {
            int h = p.getDataHoraEntrada().getHour();
            qtd[h]++;
            valor[h] = valor[h].add(valorLiquido(p));
        }
        List<HoraVenda> lista = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            lista.add(new HoraVenda(h, qtd[h], valor[h].setScale(2, RoundingMode.HALF_UP)));
        }
        return lista;
    }

    // ------------------------------------------------------------------ RF03

    public List<StatusQuantidade> situacaoPedidos() {
        Map<StatusPedido, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido s : StatusPedido.values()) {
            contagem.put(s, 0L);
        }
        for (Pedido p : pedidos) {
            if (p.getStatus() != null) {
                contagem.merge(p.getStatus(), 1L, Long::sum);
            }
        }
        long total = pedidos.size();
        return contagem.entrySet().stream()
                .map(e -> new StatusQuantidade(e.getKey().name(), e.getValue(),
                        total == 0 ? zero() : BigDecimal.valueOf(e.getValue()).multiply(CEM)
                                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)))
                .toList();
    }

    // ------------------------------------------------------------------ RF04

    public List<ProdutoRanking> topProdutos(int limite, String ordenarPor) {
        boolean porFaturamento = "faturamento".equalsIgnoreCase(ordenarPor);
        Map<String, Object[]> mapa = new LinkedHashMap<>(); // chave -> [nome, categoria, qtd, valor]
        for (Pedido p : validos()) {
            BigDecimal fator = fatorDesconto(p);
            for (ItemPedido i : itensConsiderados(p)) {
                String chave = i.getProdutoId() != null ? i.getProdutoId() : i.getNomeProduto();
                Object[] acc = mapa.computeIfAbsent(chave, x -> new Object[]{i.getNomeProduto(), categoria(i), 0L, BigDecimal.ZERO});
                acc[2] = (Long) acc[2] + i.getQuantidade();
                acc[3] = ((BigDecimal) acc[3]).add(i.calcularSubtotal().multiply(fator));
            }
        }

        Comparator<Map.Entry<String, Object[]>> porQtd = Comparator.comparing(e -> (Long) e.getValue()[2]);
        Comparator<Map.Entry<String, Object[]>> porValor = Comparator.comparing(e -> (BigDecimal) e.getValue()[3]);
        Comparator<Map.Entry<String, Object[]>> ordem = porFaturamento
                ? porValor.reversed().thenComparing(porQtd.reversed())
                : porQtd.reversed().thenComparing(porValor.reversed());

        List<Map.Entry<String, Object[]>> ordenados = mapa.entrySet().stream().sorted(ordem).limit(limite).toList();
        List<ProdutoRanking> ranking = new ArrayList<>();
        for (int idx = 0; idx < ordenados.size(); idx++) {
            Map.Entry<String, Object[]> e = ordenados.get(idx);
            Object[] v = e.getValue();
            ranking.add(new ProdutoRanking(idx + 1, e.getKey(), (String) v[0], (String) v[1], (Long) v[2],
                    ((BigDecimal) v[3]).setScale(2, RoundingMode.HALF_UP)));
        }
        return ranking;
    }

    // ------------------------------------------------------------------ apoio

    static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null; // sem base de comparação
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private List<Pedido> validos() {
        return pedidos.stream().filter(p -> p.getStatus() != StatusPedido.CANCELADO).toList();
    }

    private List<ItemPedido> itensConsiderados(Pedido p) {
        if (categoriaFiltro == null) {
            return p.getItens();
        }
        return p.getItens().stream().filter(i -> categoria(i).equalsIgnoreCase(categoriaFiltro)).toList();
    }

    private String categoria(ItemPedido i) {
        String c = i.getProdutoId() != null ? categoriaPorProduto.get(i.getProdutoId()) : null;
        return c == null || c.isBlank() ? SEM_CATEGORIA : c;
    }

    private BigDecimal valorBruto(Pedido p) {
        return itensConsiderados(p).stream().map(ItemPedido::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorLiquido(Pedido p) {
        return valorBruto(p).multiply(fatorDesconto(p));
    }

    private BigDecimal fatorDesconto(Pedido p) {
        BigDecimal perc = p.getPercentualDesconto();
        if (perc == null || perc.signum() <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(perc.divide(CEM, 6, RoundingMode.HALF_UP));
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
