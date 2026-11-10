package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            garantirUsuario(repo, encoder, "admin", Perfil.ADMIN);
            garantirUsuario(repo, encoder, "gerente", Perfil.GERENTE);
            garantirUsuario(repo, encoder, "atendente", Perfil.ATENDENTE);
        };
    }

    @Bean
    CommandLineRunner initProdutos(ProdutoRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(catalogoPadrao());
            }
        };
    }

    /**
     * Gera uma massa de pedidos de demonstração (últimos 90 dias) para o dashboard
     * ter dados para exibir. Só roda com o banco de pedidos vazio e pode ser
     * desligada com app.seed.demo=false.
     */
    @Bean
    CommandLineRunner initPedidos(ClienteRepository clienteRepo, ProdutoRepository produtoRepo,
                                  PedidoRepository pedidoRepo,
                                  @Value("${app.seed.demo:true}") boolean gerarDemo) {
        return args -> {
            if (!gerarDemo || pedidoRepo.count() > 0) {
                return;
            }

            List<Produto> cardapio = produtoRepo.findAll();
            if (cardapio.isEmpty()) {
                cardapio = produtoRepo.saveAll(catalogoPadrao());
            }

            List<Cliente> clientes = clienteRepo.findAll();
            if (clientes.isEmpty()) {
                clientes = clienteRepo.saveAll(clientesIniciais());
            }

            pedidoRepo.saveAll(gerarPedidosDemo(cardapio, clientes, 90));
        };
    }

    List<Pedido> gerarPedidosDemo(List<Produto> cardapio, List<Cliente> clientes, int dias) {
        Random rnd = new Random(42); // semente fixa: mesma massa de dados a cada execução
        String[] atendentes = {"atendente", "gerente", "admin"};
        int[] horasPico = {11, 12, 12, 13, 13, 14, 18, 19, 19, 20, 20, 20, 21, 21, 22, 23};
        String[] motivos = {"Cliente desistiu", "Endereço fora da área de entrega", "Pedido duplicado",
                "Demora no preparo", "Produto indisponível"};

        LocalDate hoje = LocalDate.now();
        List<Pedido> pedidos = new ArrayList<>();

        for (int d = dias - 1; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            boolean fimDeSemana = dia.getDayOfWeek().getValue() >= 5; // sexta, sábado e domingo vendem mais
            int qtdPedidos = (fimDeSemana ? 5 : 2) + rnd.nextInt(fimDeSemana ? 5 : 4);

            for (int n = 0; n < qtdPedidos; n++) {
                LocalDateTime entrada = dia.atTime(horasPico[rnd.nextInt(horasPico.length)], rnd.nextInt(60));
                if (entrada.isAfter(LocalDateTime.now())) {
                    entrada = LocalDateTime.now().minusMinutes(5L + rnd.nextInt(120));
                }
                Cliente cliente = clientes.get(rnd.nextInt(clientes.size()));

                Pedido p = new Pedido();
                p.setClienteId(cliente.getId());
                p.setClienteNome(cliente.getNome());
                p.setResponsavelEntrada(atendentes[rnd.nextInt(atendentes.length)]);
                p.setDataHoraEntrada(entrada);

                int qtdItens = 1 + rnd.nextInt(3);
                for (int i = 0; i < qtdItens; i++) {
                    Produto prod = cardapio.get(rnd.nextInt(cardapio.size()));
                    p.getItens().add(new ItemPedido(prod.getId(), prod.getNome(), 1 + rnd.nextInt(2),
                            BigDecimal.valueOf(prod.getPreco())));
                }
                p.setStatus(StatusPedido.RECEBIDO);
                p.recalcularValores();

                if (rnd.nextInt(10) == 0) {
                    p.aplicarDesconto(BigDecimal.valueOf(5 + rnd.nextInt(4) * 5L)); // 5, 10, 15 ou 20%
                }

                boolean recente = entrada.toLocalDate().equals(hoje);
                int sorteio = rnd.nextInt(100);
                if (sorteio < 7) {
                    p.cancelar(motivos[rnd.nextInt(motivos.length)], "gerente");
                    p.setDataHoraCancelamento(entrada.plusMinutes(10));
                } else if (recente) {
                    StatusPedido[] andamento = {StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO};
                    avancarAte(p, andamento[rnd.nextInt(andamento.length)], entrada, rnd);
                } else {
                    avancarAte(p, StatusPedido.FINALIZADO, entrada, rnd);
                }
                pedidos.add(p);
            }
        }
        return pedidos;
    }

    private void avancarAte(Pedido p, StatusPedido alvo, LocalDateTime entrada, Random rnd) {
        if (alvo == StatusPedido.RECEBIDO) {
            return;
        }
        p.evoluirStatus(StatusPedido.EM_PREPARACAO, "cozinha");
        if (alvo == StatusPedido.EM_PREPARACAO) {
            return;
        }
        p.evoluirStatus(StatusPedido.PRONTO, "cozinha");
        if (alvo == StatusPedido.PRONTO) {
            return;
        }
        boolean entrega = rnd.nextInt(3) > 0;
        p.evoluirStatus(entrega ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO, entrega ? "entregador" : "balcao");
        p.setDataHoraSaida(entrada.plusMinutes(25L + rnd.nextInt(30)));
        p.evoluirStatus(StatusPedido.FINALIZADO, "sistema");
    }

    private List<Produto> catalogoPadrao() {
        return List.of(
                criarProduto("Pizza Calabresa Especial", "Pizza", 48.0),
                criarProduto("Pizza Quatro Queijos", "Pizza", 52.0),
                criarProduto("Pizza Margherita", "Pizza", 46.0),
                criarProduto("Pizza Frango com Catupiry", "Pizza", 54.0),
                criarProduto("Esfiha de Carne", "Esfiha", 7.5),
                criarProduto("Esfiha de Queijo", "Esfiha", 7.0),
                criarProduto("X-Burger Artesanal", "Lanche", 32.0),
                criarProduto("X-Salada", "Lanche", 28.0),
                criarProduto("Refrigerante Guaraná 2L", "Bebida", 14.0),
                criarProduto("Suco Natural 500ml", "Bebida", 11.0),
                criarProduto("Pizza Doce de Chocolate", "Sobremesa", 42.0),
                criarProduto("Petit Gâteau", "Sobremesa", 22.0)
        );
    }

    private void garantirUsuario(UsuarioRepository repo, PasswordEncoder encoder, String username, Perfil role) {
        Usuario usuario = repo.findByUsername(username).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword(encoder.encode("teste123"));
            return u;
        });
        if (usuario.getRole() == null) {
            usuario.setRole(role);
        }
        repo.save(usuario);
    }

    private Produto criarProduto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    private List<Cliente> clientesIniciais() {
        List<Cliente> base = new ArrayList<>();
        String[][] dados = {
                {"Consumidor Balcão", "11988887777", "Mogi das Cruzes"},
                {"Ana Souza", "11991112233", "Mogi das Cruzes"},
                {"Bruno Lima", "11992223344", "Suzano"},
                {"Carla Mendes", "11993334455", "Mogi das Cruzes"},
                {"Diego Rocha", "11994445566", "Itaquaquecetuba"},
                {"Eduarda Alves", "11995556677", "Suzano"},
                {"Felipe Costa", "11996667788", "Mogi das Cruzes"},
                {"Gabriela Nunes", "11997778899", "Mogi das Cruzes"}
        };
        for (String[] d : dados) {
            Cliente c = new Cliente();
            c.setNome(d[0]);
            c.setTelefone(d[1]);
            c.setCidade(d[2]);
            c.setEstado("SP");
            base.add(c);
        }
        return base;
    }
}
