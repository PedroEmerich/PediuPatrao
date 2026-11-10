package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.*;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardFiltroTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Test
    @DisplayName("Sem datas, considera os últimos 30 dias até hoje")
    void periodoPadrao() {
        DashboardFiltro f = DashboardFiltro.de(null, null, null, null, null, HOJE);
        assertEquals(HOJE, f.dataFim());
        assertEquals(LocalDate.of(2026, 10, 12), f.dataInicio());
        assertEquals(30, f.quantidadeDias());
    }

    @Test
    @DisplayName("Converte status sem diferenciar maiúsculas/minúsculas")
    void statusCaseInsensitive() {
        DashboardFiltro f = DashboardFiltro.de("2026-11-01", "2026-11-05", "finalizado", " Pizza ", "", HOJE);
        assertEquals(StatusPedido.FINALIZADO, f.status());
        assertEquals("Pizza", f.categoria());
        assertNull(f.clienteId());
    }

    @Test
    @DisplayName("Rejeita data em formato inválido")
    void dataInvalida() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DashboardFiltro.de("10/11/2026", null, null, null, null, HOJE));
        assertTrue(e.getMessage().contains("dataInicio"));
    }

    @Test
    @DisplayName("Rejeita dataInicio posterior a dataFim")
    void periodoInvertido() {
        assertThrows(IllegalArgumentException.class,
                () -> DashboardFiltro.de("2026-11-10", "2026-11-01", null, null, null, HOJE));
    }

    @Test
    @DisplayName("Rejeita período maior que 366 dias")
    void periodoMuitoLongo() {
        assertThrows(IllegalArgumentException.class,
                () -> DashboardFiltro.de("2024-01-01", "2026-01-01", null, null, null, HOJE));
    }

    @Test
    @DisplayName("Rejeita status inexistente")
    void statusInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> DashboardFiltro.de(null, null, "ENTREGUE_ONTEM", null, null, HOJE));
    }

    @Test
    @DisplayName("Período anterior tem a mesma duração e termina um dia antes do início")
    void periodoAnterior() {
        DashboardFiltro f = DashboardFiltro.de("2026-11-08", "2026-11-10", null, null, null, HOJE);
        DashboardFiltro ant = f.periodoAnterior();
        assertEquals(LocalDate.of(2026, 11, 5), ant.dataInicio());
        assertEquals(LocalDate.of(2026, 11, 7), ant.dataFim());
    }
}
