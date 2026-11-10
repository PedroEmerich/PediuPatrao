package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String usuario;
    private String perfil;
    private LocalDateTime dataHora;
    private String operacao;
    private String entidade;
    private String entidadeId;
    private List<Alteracao> alteracoes = new ArrayList<>();
    private String justificativa;
}
