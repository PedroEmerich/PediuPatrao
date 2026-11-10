package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta dos registros de auditoria (perfis ADMIN e GERENTE). */
@RestController
@RequestMapping({"/api/auditoria", "/api/v1/auditoria"})
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistroAuditoria> listar(@RequestParam(required = false) String usuario,
                                          @RequestParam(required = false) String operacao,
                                          @RequestParam(required = false) String entidade) {
        return service.listar().stream()
                .filter(r -> usuario == null || usuario.isBlank() || usuario.equalsIgnoreCase(r.getUsuario()))
                .filter(r -> operacao == null || operacao.isBlank() || operacao.equalsIgnoreCase(r.getOperacao()))
                .filter(r -> entidade == null || entidade.isBlank() || entidade.equalsIgnoreCase(r.getEntidade()))
                .toList();
    }
}
