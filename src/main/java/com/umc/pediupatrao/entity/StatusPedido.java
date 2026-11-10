package com.umc.pediupatrao.entity;

import java.util.EnumSet;
import java.util.Set;

public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    private static final Set<StatusPedido> ESTAGIOS_INICIAIS = EnumSet.of(
            RECEBIDO,
            EM_PREPARACAO,
            PRONTO
    );

    public boolean podeAvancarPara(StatusPedido destino) {
        if (destino == null) return false;
        return switch (this) {
            case RECEBIDO -> destino == EM_PREPARACAO;
            case EM_PREPARACAO -> destino == PRONTO;
            case PRONTO -> destino == SAIU_PARA_ENTREGA || destino == RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> destino == FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return podeAvancarPara(novo);
    }

    public boolean isFaseInicial() {
        return ESTAGIOS_INICIAIS.contains(this);
    }

    public boolean antesDaSaida() {
        return isFaseInicial();
    }
}
