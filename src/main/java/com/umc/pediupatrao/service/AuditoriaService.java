package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) {
        this.repo = repo;
    }

    public String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
    }

    private String extrairRoleAtual(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return "DESCONHECIDO";
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if (role != null) {
                return role.replace("ROLE_", "");
            }
        }
        return "DESCONHECIDO";
    }

    public void registrar(String operacao, String entidade, String entidadeId, List<Alteracao> alteracoes, String justificativa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
        String perfil = extrairRoleAtual(auth);

        RegistroAuditoria entrada = new RegistroAuditoria();
        entrada.setUsuario(usuario);
        entrada.setPerfil(perfil);
        entrada.setDataHora(LocalDateTime.now());
        entrada.setOperacao(operacao);
        entrada.setEntidade(entidade);
        entrada.setEntidadeId(entidadeId);
        entrada.setAlteracoes(alteracoes != null ? alteracoes : Collections.emptyList());
        entrada.setJustificativa(justificativa);

        repo.insert(entrada);
    }

    public List<RegistroAuditoria> listar() {
        return repo.findAllByOrderByDataHoraDesc();
    }
}
