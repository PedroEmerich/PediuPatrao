package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;
    private int quantidade;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal precoUnitario;

    public BigDecimal calcularSubtotal() {
        if (precoUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSubtotal() {
        return calcularSubtotal();
    }
}
