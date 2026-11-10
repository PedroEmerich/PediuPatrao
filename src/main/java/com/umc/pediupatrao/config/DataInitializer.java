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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Bean
    CommandLineRunner initPedidos(ClienteRepository clienteRepo, ProdutoRepository produtoRepo,
                                  PedidoRepository pedidoRepo) {
        return args -> {
            if (pedidoRepo.count() > 0) {
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

            Cliente principal = clientes.get(0);
            Produto p1 = cardapio.get(0);
            Produto p2 = cardapio.size() > 1 ? cardapio.get(1) : p1;

            Pedido exemplo = new Pedido();
            exemplo.setClienteId(principal.getId());
            exemplo.setClienteNome(principal.getNome());
            exemplo.setResponsavelEntrada("admin");
            exemplo.setDataHoraEntrada(LocalDateTime.now().minusHours(2));
            exemplo.setStatus(StatusPedido.RECEBIDO);

            exemplo.getItens().add(new ItemPedido(p1.getId(), p1.getNome(), 1, BigDecimal.valueOf(p1.getPreco())));
            exemplo.getItens().add(new ItemPedido(p2.getId(), p2.getNome(), 2, BigDecimal.valueOf(p2.getPreco())));
            exemplo.recalcularValores();

            pedidoRepo.save(exemplo);
        };
    }

    private List<Produto> catalogoPadrao() {
        return List.of(
                criarProduto("Pizza Calabresa Especial", "Pizza", 48.0),
                criarProduto("Pizza Quatro Queijos", "Pizza", 52.0),
                criarProduto("Refrigerante Guaraná 2L", "Bebida", 14.0)
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
        Cliente c1 = new Cliente();
        c1.setNome("Consumidor Balcão");
        c1.setTelefone("11988887777");
        base.add(c1);
        return base;
    }
}
