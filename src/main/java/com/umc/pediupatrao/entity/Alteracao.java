package com.umc.pediupatrao.entity;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alteracao {

    private String campo;
    private String valorAnterior;
    private String valorNovo;

    public static Alteracao de(String propriedade, Object antes, Object depois) {
        return new Alteracao(
                propriedade,
                antes != null ? String.valueOf(antes) : null,
                depois != null ? String.valueOf(depois) : null
        );
    }

    public static void registrarSeDiferente(java.util.List<Alteracao> historico, String propriedade, Object antes, Object depois) {
        String strAntes = antes != null ? String.valueOf(antes) : "";
        String strDepois = depois != null ? String.valueOf(depois) : "";
        if (!Objects.equals(strAntes, strDepois)) {
            historico.add(new Alteracao(propriedade, antes != null ? strAntes : null, depois != null ? strDepois : null));
        }
    }

    public static void comparar(java.util.List<Alteracao> lista, String campo, String anterior, String novo) {
        registrarSeDiferente(lista, campo, anterior, novo);
    }
}
