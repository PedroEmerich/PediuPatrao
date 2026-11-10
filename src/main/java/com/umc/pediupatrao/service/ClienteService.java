package com.umc.pediupatrao.service;
import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private AuditoriaService auditoria;
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow();
        clienteRepository.delete(cliente);
        auditoria.registrar("EXCLUSAO_CLIENTE", "Cliente", id, List.of(Alteracao.de("nome", cliente.getNome(), null)), null);
    }
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);
            auditoria.registrar("CRIACAO_CLIENTE", "Cliente", salvo.getId(), List.of(), null);
            return salvo;
        }
        Cliente antigo = clienteRepository.findById(cliente.getId()).orElseThrow();
        List<Alteracao> alteracoes = new ArrayList<>();
        Alteracao.comparar(alteracoes, "nome", antigo.getNome(), cliente.getNome());
        Alteracao.comparar(alteracoes, "telefone", antigo.getTelefone(), cliente.getTelefone());
        Alteracao.comparar(alteracoes, "cep", antigo.getCep(), cliente.getCep());
        Alteracao.comparar(alteracoes, "logradouro", antigo.getLogradouro(), cliente.getLogradouro());
        Alteracao.comparar(alteracoes, "numero", antigo.getNumero(), cliente.getNumero());
        Alteracao.comparar(alteracoes, "complemento", antigo.getComplemento(), cliente.getComplemento());
        Alteracao.comparar(alteracoes, "bairro", antigo.getBairro(), cliente.getBairro());
        Alteracao.comparar(alteracoes, "cidade", antigo.getCidade(), cliente.getCidade());
        Alteracao.comparar(alteracoes, "estado", antigo.getEstado(), cliente.getEstado());
        Cliente salvo = clienteRepository.save(cliente);
        auditoria.registrar("ALTERACAO_CLIENTE", "Cliente", salvo.getId(), alteracoes, null);
        return salvo;
    }
}