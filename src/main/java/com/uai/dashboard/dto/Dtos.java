package com.uai.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Dtos {

    private Dtos() {}

    public record BancoResumen(
            int idBanco, String nombre,
            long total, long aprobadas, long rechazadas, long pendientes, long revertidas,
            BigDecimal montoAprobado, BigDecimal tasaAprobacion,
            String semaforo) {}

    public record Totales(
            long total, long aprobadas, long rechazadas, long pendientes, long revertidas,
            BigDecimal montoAprobado, BigDecimal tasaAprobacion) {}

    public record EvolucionDia(LocalDate dia, long total, long aprobadas, long rechazadas) {}

    public record TransferenciaFila(
            long idTransferencia, LocalDateTime fecha, String producto, String pais,
            BigDecimal monto, String moneda, String estado) {}

    public record TransferenciaDetalle(
            long idTransferencia, int idBanco, String banco, String productoCodigo, String producto,
            String pais, boolean paisLocal, BigDecimal monto, String moneda, String estado,
            String codigoRespuesta, String descripcionRespuesta, LocalDateTime fecha) {}

    public record Opcion(int id, String descripcion) {}
}
