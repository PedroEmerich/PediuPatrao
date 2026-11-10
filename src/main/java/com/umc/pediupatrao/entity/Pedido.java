package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@Setter
@Document(collection = "pedidos")
public class Pedido {

    public static final BigDecimal TETO_DESCONTO_PERCENTUAL = BigDecimal.valueOf(20);

    @Id
    private String id;
    private String clienteId;
    private String clienteNome;

    private List<ItemPedido> itens = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentualDesconto = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal total = BigDecimal.ZERO;

    private StatusPedido status;
    private String responsavelEntrada;
    private LocalDateTime dataHoraEntrada;
    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;
    private String canceladoPor;
    private LocalDateTime dataHoraCancelamento;
    private String justificativaCancelamento;

    public Pedido() {
        this.itens = new ArrayList<>();
    }

    public void recalcularValores() {
        if (itens == null || itens.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.total = BigDecimal.ZERO;
            return;
        }

        BigDecimal acumulador = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            acumulador = acumulador.add(item.calcularSubtotal());
        }
        this.subtotal = acumulador.setScale(2, RoundingMode.HALF_UP);

        if (this.percentualDesconto != null && this.percentualDesconto.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fator = this.percentualDesconto.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal abatimento = this.subtotal.multiply(fator).setScale(2, RoundingMode.HALF_UP);
            this.total = this.subtotal.subtract(abatimento).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.total = this.subtotal;
        }
    }

    // Compatibilidade com código legado / testes existentes
    public void calcularTotal() {
        recalcularValores();
    }

    public void evoluirStatus(StatusPedido proximoStatus, String operador) {
        if (this.status == null) {
            this.status = proximoStatus;
            return;
        }

        if (!this.status.podeAvancarPara(proximoStatus)) {
            throw new IllegalStateException("Mudança de estágio proibida de " + this.status + " para " + proximoStatus);
        }

        this.status = proximoStatus;
        if (proximoStatus == StatusPedido.SAIU_PARA_ENTREGA || proximoStatus == StatusPedido.RETIRADO) {
            this.responsavelSaida = operador;
            this.dataHoraSaida = LocalDateTime.now();
        }
    }

    public void mudarStatus(StatusPedido novo, String usuario) {
        evoluirStatus(novo, usuario);
    }

    public void aplicarDesconto(BigDecimal taxa) {
        if (!podeModificar()) {
            throw new IllegalStateException("Desconto só é permitido antes da expedição do pedido");
        }

        if (taxa == null || taxa.compareTo(BigDecimal.ZERO) <= 0 || taxa.compareTo(TETO_DESCONTO_PERCENTUAL) > 0) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 0.01% e 20.00%");
        }

        this.percentualDesconto = taxa.setScale(2, RoundingMode.HALF_UP);
        recalcularValores();
    }

    public void cancelar(String motivo, String operador) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("A justificativa de cancelamento é obrigatória");
        }

        if (!podeModificar()) {
            throw new IllegalStateException("Não é possível cancelar pedido em trânsito ou finalizado");
        }

        this.status = StatusPedido.CANCELADO;
        this.justificativaCancelamento = motivo.trim();
        this.canceladoPor = operador;
        this.dataHoraCancelamento = LocalDateTime.now();
    }

    public boolean podeModificar() {
        return this.status != null && this.status.isFaseInicial();
    }

    public List<ItemPedido> getItens() {
        if (this.itens == null) {
            this.itens = new ArrayList<>();
        }
        return this.itens;
    }
}
