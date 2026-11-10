package com.umc.pediupatrao;

import static org.junit.jupiter.api.Assertions.*;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private Pedido fabricarPedidoBase() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.getItens().add(new ItemPedido("prod-1", "Pizza Artesanal", 2, new BigDecimal("50.00")));
        pedido.recalcularValores();
        return pedido;
    }

    @Test
    @DisplayName("Deve computar corretamente o subtotal e total inicial")
    void deveComputarSubtotalETotalInicial() {
        Pedido p = fabricarPedidoBase();
        assertEquals(0, new BigDecimal("100.00").compareTo(p.getTotal()));
        assertEquals(0, new BigDecimal("100.00").compareTo(p.getSubtotal()));
    }

    @Test
    @DisplayName("Deve conceder abatimento percentual com sucesso")
    void deveConcederAbatimentoPercentual() {
        Pedido p = fabricarPedidoBase();
        p.aplicarDesconto(new BigDecimal("10.00"));
        assertEquals(0, new BigDecimal("90.00").compareTo(p.getTotal()));
    }

    @Test
    @DisplayName("Deve rejeitar concessão de desconto superior a vinte porcento")
    void deveRejeitarDescontoAcimaDoLimite() {
        Pedido p = fabricarPedidoBase();
        assertThrows(IllegalArgumentException.class, () -> p.aplicarDesconto(new BigDecimal("21")));
    }

    @Test
    @DisplayName("Deve impedir saltos ilegais no fluxo de preparação")
    void deveImpedirSaltosInvalidosDeStatus() {
        Pedido p = fabricarPedidoBase();
        assertThrows(IllegalStateException.class, () -> p.evoluirStatus(StatusPedido.FINALIZADO, "operador-1"));
    }

    @Test
    @DisplayName("Deve registrar horário de expedição e bloquear alterações posteriores")
    void deveRegistrarExpedicaoEBloquearModificacoes() {
        Pedido p = fabricarPedidoBase();
        p.evoluirStatus(StatusPedido.EM_PREPARACAO, "cozinha");
        p.evoluirStatus(StatusPedido.PRONTO, "cozinha");
        p.evoluirStatus(StatusPedido.SAIU_PARA_ENTREGA, "motoboy");

        assertEquals("motoboy", p.getResponsavelSaida());
        assertNotNull(p.getDataHoraSaida());
        assertThrows(IllegalStateException.class, () -> p.aplicarDesconto(BigDecimal.valueOf(5)));
        assertThrows(IllegalStateException.class, () -> p.cancelar("Desistência tardia", "gerencia"));
    }

    @Test
    @DisplayName("Deve congelar pedido após cancelamento")
    void deveCongelarPedidoCancelado() {
        Pedido p = fabricarPedidoBase();
        p.cancelar("Cliente solicitou estorno", "atendente");
        assertThrows(IllegalStateException.class, () -> p.evoluirStatus(StatusPedido.EM_PREPARACAO, "cozinha"));
    }

    @Test
    @DisplayName("Deve exigir justificativa textual para efetuar o cancelamento")
    void deveExigirJustificativaNoCancelamento() {
        Pedido p = fabricarPedidoBase();
        assertThrows(IllegalArgumentException.class, () -> p.cancelar("", "operador"));
        assertThrows(IllegalArgumentException.class, () -> p.cancelar(null, "operador"));
    }
}
