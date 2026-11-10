package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardDtos.CategoriaVenda;
import com.umc.pediupatrao.dto.DashboardDtos.HoraVenda;
import com.umc.pediupatrao.dto.DashboardDtos.OpcoesFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.Painel;
import com.umc.pediupatrao.dto.DashboardDtos.PontoVenda;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.Resumo;
import com.umc.pediupatrao.dto.DashboardDtos.StatusQuantidade;
import com.umc.pediupatrao.service.DashboardFiltro;
import com.umc.pediupatrao.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do Dashboard Administrativo (RF01 a RF07).
 * Acesso restrito aos perfis ADMIN e GERENTE (ver SecurityConfig).
 *
 * Parâmetros de filtro comuns a todos os endpoints (todos opcionais):
 *   dataInicio, dataFim (AAAA-MM-DD), status, categoria, clienteId
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public Painel painel(@RequestParam(required = false) String dataInicio,
                         @RequestParam(required = false) String dataFim,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String categoria,
                         @RequestParam(required = false) String clienteId,
                         @RequestParam(required = false) String agrupamento,
                         @RequestParam(required = false) Integer limite) {
        return service.painel(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId), agrupamento, limite);
    }

    @GetMapping("/resumo")
    public Resumo resumo(@RequestParam(required = false) String dataInicio,
                         @RequestParam(required = false) String dataFim,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String categoria,
                         @RequestParam(required = false) String clienteId) {
        return service.resumo(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId));
    }

    @GetMapping("/vendas")
    public List<PontoVenda> vendas(@RequestParam(required = false) String dataInicio,
                                   @RequestParam(required = false) String dataFim,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String categoria,
                                   @RequestParam(required = false) String clienteId,
                                   @RequestParam(required = false) String agrupamento) {
        return service.vendas(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId), agrupamento);
    }

    @GetMapping("/status")
    public List<StatusQuantidade> status(@RequestParam(required = false) String dataInicio,
                                         @RequestParam(required = false) String dataFim,
                                         @RequestParam(required = false) String categoria,
                                         @RequestParam(required = false) String clienteId) {
        return service.situacao(DashboardFiltro.de(dataInicio, dataFim, null, categoria, clienteId));
    }

    @GetMapping("/produtos/top")
    public List<ProdutoRanking> topProdutos(@RequestParam(required = false) String dataInicio,
                                            @RequestParam(required = false) String dataFim,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String categoria,
                                            @RequestParam(required = false) String clienteId,
                                            @RequestParam(required = false) Integer limite,
                                            @RequestParam(required = false) String ordenarPor) {
        return service.topProdutos(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId), limite, ordenarPor);
    }

    @GetMapping("/categorias")
    public List<CategoriaVenda> categorias(@RequestParam(required = false) String dataInicio,
                                           @RequestParam(required = false) String dataFim,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String categoria,
                                           @RequestParam(required = false) String clienteId) {
        return service.categorias(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId));
    }

    @GetMapping("/horarios")
    public List<HoraVenda> horarios(@RequestParam(required = false) String dataInicio,
                                    @RequestParam(required = false) String dataFim,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String categoria,
                                    @RequestParam(required = false) String clienteId) {
        return service.horarios(DashboardFiltro.de(dataInicio, dataFim, status, categoria, clienteId));
    }

    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return service.opcoesFiltro();
    }
}
