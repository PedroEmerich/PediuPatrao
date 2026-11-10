package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.Resumo;
import com.umc.pediupatrao.service.DashboardFiltro;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Testes da camada HTTP: contrato JSON, validações (400) e permissões (401/403). */
@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static Resumo resumoExemplo() {
        return new Resumo(new BigDecimal("194.00"), 4, 3, 1, 1, 2, new BigDecimal("64.67"),
                new BigDecimal("25.00"), new BigDecimal("6.00"), 7, new BigDecimal("12.50"));
    }

    @Test
    @DisplayName("GET /resumo como GERENTE retorna 200 com os indicadores")
    void resumoGerente() throws Exception {
        when(service.resumo(any(DashboardFiltro.class))).thenReturn(resumoExemplo());

        mvc.perform(get("/api/v1/dashboard/resumo").param("dataInicio", "2026-11-01").param("dataFim", "2026-11-10")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(194.00))
                .andExpect(jsonPath("$.ticketMedio").value(64.67))
                .andExpect(jsonPath("$.pedidosCancelados").value(1));
    }

    @Test
    @DisplayName("GET /produtos/top como ADMIN retorna o ranking")
    void topProdutosAdmin() throws Exception {
        when(service.topProdutos(any(DashboardFiltro.class), eq(3), anyString()))
                .thenReturn(List.of(new ProdutoRanking(1, "pz1", "Calabresa", "Pizza", 10, new BigDecimal("480.00"))));

        mvc.perform(get("/api/v1/dashboard/produtos/top").param("limite", "3").param("ordenarPor", "faturamento")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Calabresa"))
                .andExpect(jsonPath("$[0].posicao").value(1));
    }

    @Test
    @DisplayName("ATENDENTE não acessa o dashboard (403)")
    void atendenteProibido() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumo").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401 (e não redireciona para o login)")
    void semLogin() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Data inválida retorna 400 com mensagem padronizada")
    void dataInvalida() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumo").param("dataInicio", "01/11/2026")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.caminho").value("/api/v1/dashboard/resumo"));
    }

    @Test
    @DisplayName("Período invertido retorna 400")
    void periodoInvertido() throws Exception {
        mvc.perform(get("/api/v1/dashboard/vendas").param("dataInicio", "2026-11-10").param("dataFim", "2026-11-01")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Limite não numérico retorna 400")
    void limiteNaoNumerico() throws Exception {
        mvc.perform(get("/api/v1/dashboard/produtos/top").param("limite", "dez")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro 'limite' com valor inválido: dez"));
    }
}
