package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

/**
 * Filtros aceitos pelos endpoints do dashboard (RF05).
 *
 * Regras:
 * - Sem datas: considera os últimos 30 dias (incluindo hoje).
 * - Só dataInicio: até hoje. Só dataFim: 30 dias antes dela.
 * - dataInicio não pode ser posterior a dataFim.
 * - Intervalo máximo de 366 dias (evita consultas pesadas).
 */
public record DashboardFiltro(
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusPedido status,
        String categoria,
        String clienteId
) {

    public static final int DIAS_PADRAO = 30;
    public static final int DIAS_MAXIMO = 366;

    public static DashboardFiltro de(String dataInicio, String dataFim, String status, String categoria, String clienteId) {
        return de(dataInicio, dataFim, status, categoria, clienteId, LocalDate.now());
    }

    static DashboardFiltro de(String dataInicio, String dataFim, String status, String categoria,
                              String clienteId, LocalDate hoje) {
        LocalDate fim = parseData(dataFim, "dataFim");
        LocalDate inicio = parseData(dataInicio, "dataInicio");

        if (fim == null) {
            fim = inicio != null && inicio.isAfter(hoje) ? inicio : hoje;
        }
        if (inicio == null) {
            inicio = fim.minusDays(DIAS_PADRAO - 1L);
        }
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("dataInicio (" + inicio + ") não pode ser posterior a dataFim (" + fim + ")");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > DIAS_MAXIMO) {
            throw new IllegalArgumentException("O período consultado não pode ultrapassar " + DIAS_MAXIMO + " dias");
        }

        return new DashboardFiltro(inicio, fim, parseStatus(status), vazioParaNulo(categoria), vazioParaNulo(clienteId));
    }

    /** Mesmo filtro deslocado para o período imediatamente anterior (para comparação). */
    public DashboardFiltro periodoAnterior() {
        long dias = quantidadeDias();
        return new DashboardFiltro(dataInicio.minusDays(dias), dataInicio.minusDays(1), status, categoria, clienteId);
    }

    public long quantidadeDias() {
        return ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
    }

    public LocalDateTime inicioDateTime() {
        return dataInicio.atStartOfDay();
    }

    /** Limite superior exclusivo (início do dia seguinte a dataFim). */
    public LocalDateTime fimDateTimeExclusivo() {
        return dataFim.plusDays(1).atStartOfDay();
    }

    private static LocalDate parseData(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Parâmetro '" + campo + "' inválido: use o formato AAAA-MM-DD (ex.: 2026-11-01)");
        }
    }

    private static StatusPedido parseStatus(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return StatusPedido.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: '" + valor + "'. Valores aceitos: "
                    + Arrays.toString(StatusPedido.values()));
        }
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
