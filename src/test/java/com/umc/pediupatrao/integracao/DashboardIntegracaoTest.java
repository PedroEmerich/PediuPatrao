package com.umc.pediupatrao.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de integração ponta a ponta: API -> Service -> MongoDB real.
 * Usa o banco "pizzaria_test" (separado do banco da aplicação).
 * Só executa quando a variável MONGO_URI estiver definida.
 */
@SpringBootTest(properties = {"spring.data.mongodb.database=pizzaria_test", "app.seed.demo=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGO_URI", matches = ".+")
class DashboardIntegracaoTest {

    @Autowired private MockMvc mvc;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private ProdutoRepository produtoRepo;
    @Autowired private ClienteRepository clienteRepo;

    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void prepararBanco() {
        pedidoRepo.deleteAll();
        produtoRepo.deleteAll();
        clienteRepo.deleteAll();

        Produto pizza = novoProduto("Pizza Teste", "Pizza", 50.0);
        Produto refri = novoProduto("Refri Teste", "Bebida", 10.0);
        Cliente cli = new Cliente();
        cli.setNome("Cliente Teste");
        cli = clienteRepo.save(cli);

        pedidoRepo.save(novoPedido(cli, hoje, 19, StatusPedido.FINALIZADO,
                new ItemPedido(pizza.getId(), pizza.getNome(), 2, new BigDecimal("50.00")),
                new ItemPedido(refri.getId(), refri.getNome(), 1, new BigDecimal("10.00"))));
        pedidoRepo.save(novoPedido(cli, hoje.minusDays(1), 12, StatusPedido.RECEBIDO,
                new ItemPedido(refri.getId(), refri.getNome(), 3, new BigDecimal("10.00"))));
        pedidoRepo.save(novoPedido(cli, hoje.minusDays(2), 20, StatusPedido.CANCELADO,
                new ItemPedido(pizza.getId(), pizza.getNome(), 4, new BigDecimal("50.00"))));
        // fora do período de 7 dias
        pedidoRepo.save(novoPedido(cli, hoje.minusDays(20), 20, StatusPedido.FINALIZADO,
                new ItemPedido(pizza.getId(), pizza.getNome(), 1, new BigDecimal("50.00"))));
    }

    private Produto novoProduto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return produtoRepo.save(p);
    }

    private Pedido novoPedido(Cliente c, LocalDate dia, int hora, StatusPedido status, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setClienteId(c.getId());
        p.setClienteNome(c.getNome());
        p.setDataHoraEntrada(dia.atTime(hora, 0));
        p.getItens().addAll(java.util.List.of(itens));
        p.recalcularValores();
        p.setStatus(status);
        return p;
    }

    @Test
    @DisplayName("Painel completo dos últimos 7 dias consultando o MongoDB")
    void painelCompleto() throws Exception {
        mvc.perform(get("/api/v1/dashboard")
                        .param("dataInicio", hoje.minusDays(6).toString())
                        .param("dataFim", hoje.toString())
                        .with(httpBasic("gerente", "teste123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.faturamento").value(140.00))
                .andExpect(jsonPath("$.resumo.totalPedidos").value(3))
                .andExpect(jsonPath("$.resumo.pedidosCancelados").value(1))
                .andExpect(jsonPath("$.vendas", hasSize(7)))
                .andExpect(jsonPath("$.topProdutos[0].nome").value("Refri Teste"))
                .andExpect(jsonPath("$.horarios", hasSize(24)));
    }

    @Test
    @DisplayName("Filtro por status e categoria aplicado direto na consulta")
    void filtros() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumo")
                        .param("dataInicio", hoje.minusDays(6).toString())
                        .param("status", "FINALIZADO")
                        .param("categoria", "Pizza")
                        .with(httpBasic("admin", "teste123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(100.00))
                .andExpect(jsonPath("$.totalPedidos").value(1));
    }

    @Test
    @DisplayName("Opções de filtro vêm do banco")
    void opcoesFiltro() throws Exception {
        mvc.perform(get("/api/v1/dashboard/filtros").with(httpBasic("gerente", "teste123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias", hasSize(2)))
                .andExpect(jsonPath("$.clientes[0].nome").value("Cliente Teste"));
    }

    @Test
    @DisplayName("Senha errada retorna 401")
    void senhaErrada() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumo").with(httpBasic("gerente", "errada")))
                .andExpect(status().isUnauthorized());
    }
}
