package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDtos.CategoriaVenda;
import com.umc.pediupatrao.dto.DashboardDtos.ClienteOpcao;
import com.umc.pediupatrao.dto.DashboardDtos.FiltroAplicado;
import com.umc.pediupatrao.dto.DashboardDtos.HoraVenda;
import com.umc.pediupatrao.dto.DashboardDtos.OpcoesFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.Painel;
import com.umc.pediupatrao.dto.DashboardDtos.PontoVenda;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.Resumo;
import com.umc.pediupatrao.dto.DashboardDtos.StatusQuantidade;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.DashboardCalculadora.Agrupamento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Busca os pedidos no MongoDB aplicando os filtros (RF05) e delega os cálculos
 * para a {@link DashboardCalculadora}.
 */
@Service
public class DashboardService {

    public static final int LIMITE_MAXIMO_RANKING = 50;

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository,
                            ClienteRepository clienteRepository) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    public Resumo resumo(DashboardFiltro filtro) {
        DashboardCalculadora anterior = calculadora(filtro.periodoAnterior());
        return calculadora(filtro).resumo(anterior.faturamento());
    }

    public List<PontoVenda> vendas(DashboardFiltro filtro, String agrupamento) {
        return calculadora(filtro).vendasPorPeriodo(filtro.dataInicio(), filtro.dataFim(), Agrupamento.de(agrupamento));
    }

    public List<StatusQuantidade> situacao(DashboardFiltro filtro) {
        return calculadora(filtro).situacaoPedidos();
    }

    public List<ProdutoRanking> topProdutos(DashboardFiltro filtro, Integer limite, String ordenarPor) {
        return calculadora(filtro).topProdutos(validarLimite(limite), validarOrdenacao(ordenarPor));
    }

    public List<CategoriaVenda> categorias(DashboardFiltro filtro) {
        return calculadora(filtro).vendasPorCategoria();
    }

    public List<HoraVenda> horarios(DashboardFiltro filtro) {
        return calculadora(filtro).vendasPorHora();
    }

    /** Endpoint consolidado: monta o painel inteiro com apenas duas consultas ao banco. */
    public Painel painel(DashboardFiltro filtro, String agrupamento, Integer limite) {
        Agrupamento agr = Agrupamento.de(agrupamento);
        int lim = validarLimite(limite);
        DashboardCalculadora calc = calculadora(filtro);
        DashboardCalculadora anterior = calculadora(filtro.periodoAnterior());
        return new Painel(
                new FiltroAplicado(filtro.dataInicio(), filtro.dataFim(),
                        filtro.status() != null ? filtro.status().name() : null, filtro.categoria(), filtro.clienteId()),
                LocalDateTime.now(),
                calc.resumo(anterior.faturamento()),
                calc.vendasPorPeriodo(filtro.dataInicio(), filtro.dataFim(), agr),
                calc.situacaoPedidos(),
                calc.topProdutos(lim, "quantidade"),
                calc.vendasPorCategoria(),
                calc.vendasPorHora());
    }

    public OpcoesFiltro opcoesFiltro() {
        List<String> categorias = produtoRepository.findAll().stream()
                .map(Produto::getTipo).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().sorted().toList();
        List<ClienteOpcao> clientes = clienteRepository.findAll().stream()
                .map(c -> new ClienteOpcao(c.getId(), c.getNome()))
                .sorted(Comparator.comparing(c -> c.nome() == null ? "" : c.nome().toLowerCase()))
                .toList();
        return new OpcoesFiltro(
                Arrays.stream(StatusPedido.values()).map(Enum::name).toList(),
                categorias,
                clientes,
                dataExtrema(Sort.Direction.ASC),
                dataExtrema(Sort.Direction.DESC));
    }

    // ------------------------------------------------------------------

    private DashboardCalculadora calculadora(DashboardFiltro filtro) {
        return new DashboardCalculadora(buscarPedidos(filtro), categoriasPorProduto(), filtro.categoria());
    }

    List<Pedido> buscarPedidos(DashboardFiltro filtro) {
        Criteria criteria = Criteria.where("dataHoraEntrada")
                .gte(filtro.inicioDateTime())
                .lt(filtro.fimDateTimeExclusivo());
        if (filtro.status() != null) {
            criteria = criteria.and("status").is(filtro.status());
        }
        if (filtro.clienteId() != null) {
            criteria = criteria.and("clienteId").is(filtro.clienteId());
        }
        return mongoTemplate.find(new Query(criteria), Pedido.class);
    }

    private Map<String, String> categoriasPorProduto() {
        Map<String, String> mapa = new HashMap<>();
        for (Produto p : produtoRepository.findAll()) {
            if (p.getId() != null && p.getTipo() != null) {
                mapa.put(p.getId(), p.getTipo());
            }
        }
        return mapa;
    }

    private LocalDate dataExtrema(Sort.Direction direcao) {
        Query q = new Query(Criteria.where("dataHoraEntrada").ne(null)).with(Sort.by(direcao, "dataHoraEntrada")).limit(1);
        Pedido p = mongoTemplate.findOne(q, Pedido.class);
        return p != null ? p.getDataHoraEntrada().toLocalDate() : null;
    }

    private static int validarLimite(Integer limite) {
        int valor = limite == null ? 5 : limite;
        if (valor < 1 || valor > LIMITE_MAXIMO_RANKING) {
            throw new IllegalArgumentException("Parâmetro 'limite' deve estar entre 1 e " + LIMITE_MAXIMO_RANKING);
        }
        return valor;
    }

    private static String validarOrdenacao(String ordenarPor) {
        if (ordenarPor == null || ordenarPor.isBlank()) {
            return "quantidade";
        }
        if (!ordenarPor.equalsIgnoreCase("quantidade") && !ordenarPor.equalsIgnoreCase("faturamento")) {
            throw new IllegalArgumentException("Parâmetro 'ordenarPor' deve ser 'quantidade' ou 'faturamento'");
        }
        return ordenarPor.toLowerCase();
    }
}
