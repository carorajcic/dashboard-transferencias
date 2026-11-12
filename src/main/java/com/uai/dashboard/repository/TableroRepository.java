package com.uai.dashboard.repository;

import com.uai.dashboard.dto.Dtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TableroRepository {

    private static final String FILTRO_FECHAS =
            " AND (CAST(:desde AS date) IS NULL OR t.fecha_creacion >= CAST(:desde AS date))"
          + " AND (CAST(:hasta AS date) IS NULL OR t.fecha_creacion <  CAST(:hasta AS date) + 1)";

    private static final String SQL_BANCOS =
            "SELECT b.id_banco, b.nombre,"
          + "       COUNT(t.id_transferencia)                                   AS total,"
          + "       COUNT(*) FILTER (WHERE t.estado = 'APROBADA')               AS aprobadas,"
          + "       COUNT(*) FILTER (WHERE t.estado = 'RECHAZADA')              AS rechazadas,"
          + "       COUNT(*) FILTER (WHERE t.estado = 'PENDIENTE')              AS pendientes,"
          + "       COUNT(*) FILTER (WHERE t.estado = 'REVERTIDA')              AS revertidas,"
          + "       COALESCE(SUM(t.monto) FILTER (WHERE t.estado = 'APROBADA'), 0) AS monto_aprobado,"
          + "       ROUND(100.0 * COUNT(*) FILTER (WHERE t.estado = 'APROBADA')"
          + "             / NULLIF(COUNT(*) FILTER (WHERE t.estado <> 'PENDIENTE'), 0), 1) AS tasa,"
          + "       CASE"
          + "         WHEN COUNT(*) FILTER (WHERE t.estado <> 'PENDIENTE') = 0 THEN 'SIN_DATOS'"
          + "         WHEN 100.0 * COUNT(*) FILTER (WHERE t.estado = 'APROBADA')"
          + "              / COUNT(*) FILTER (WHERE t.estado <> 'PENDIENTE') >= :umbralVerde    THEN 'VERDE'"
          + "         WHEN 100.0 * COUNT(*) FILTER (WHERE t.estado = 'APROBADA')"
          + "              / COUNT(*) FILTER (WHERE t.estado <> 'PENDIENTE') >= :umbralAmarillo THEN 'AMARILLO'"
          + "         ELSE 'ROJO'"
          + "       END AS semaforo"
          + " FROM bancos b"
          + " LEFT JOIN transferencias t ON t.id_banco = b.id_banco" + FILTRO_FECHAS
          + " WHERE b.habilitado = TRUE %s"
          + " GROUP BY b.id_banco, b.nombre"
          + " ORDER BY total DESC, b.nombre";

    private static final RowMapper<BancoResumen> MAP_BANCO = (rs, i) -> new BancoResumen(
            rs.getInt("id_banco"), rs.getString("nombre"),
            rs.getLong("total"), rs.getLong("aprobadas"), rs.getLong("rechazadas"),
            rs.getLong("pendientes"), rs.getLong("revertidas"),
            rs.getBigDecimal("monto_aprobado"), rs.getBigDecimal("tasa"),
            rs.getString("semaforo"));

    private final NamedParameterJdbcTemplate jdbc;
    private final BigDecimal umbralVerde;
    private final BigDecimal umbralAmarillo;

    public TableroRepository(NamedParameterJdbcTemplate jdbc,
                             @Value("${tablero.semaforo.verde}") BigDecimal umbralVerde,
                             @Value("${tablero.semaforo.amarillo}") BigDecimal umbralAmarillo) {
        this.jdbc = jdbc;
        this.umbralVerde = umbralVerde;
        this.umbralAmarillo = umbralAmarillo;
    }

    public BigDecimal getUmbralVerde() { return umbralVerde; }
    public BigDecimal getUmbralAmarillo() { return umbralAmarillo; }

    private MapSqlParameterSource fechas(LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource()
                .addValue("desde", desde == null ? null : Date.valueOf(desde))
                .addValue("hasta", hasta == null ? null : Date.valueOf(hasta))
                .addValue("umbralVerde", umbralVerde)
                .addValue("umbralAmarillo", umbralAmarillo);
    }

    // ---------------------------------------------------------------- NIVEL 1

    public List<BancoResumen> bancos(LocalDate desde, LocalDate hasta) {
        return jdbc.query(String.format(SQL_BANCOS, ""), fechas(desde, hasta), MAP_BANCO);
    }

    public Totales totales(LocalDate desde, LocalDate hasta) {
        String sql =
                "SELECT COUNT(*) AS total,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'APROBADA')  AS aprobadas,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'RECHAZADA') AS rechazadas,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'PENDIENTE') AS pendientes,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'REVERTIDA') AS revertidas,"
              + "  COALESCE(SUM(t.monto) FILTER (WHERE t.estado = 'APROBADA'), 0) AS monto_aprobado,"
              + "  ROUND(100.0 * COUNT(*) FILTER (WHERE t.estado = 'APROBADA')"
              + "        / NULLIF(COUNT(*) FILTER (WHERE t.estado <> 'PENDIENTE'), 0), 1) AS tasa"
              + " FROM transferencias t WHERE 1 = 1" + FILTRO_FECHAS;
        return jdbc.queryForObject(sql, fechas(desde, hasta), (rs, i) -> new Totales(
                rs.getLong("total"), rs.getLong("aprobadas"), rs.getLong("rechazadas"),
                rs.getLong("pendientes"), rs.getLong("revertidas"),
                rs.getBigDecimal("monto_aprobado"), rs.getBigDecimal("tasa")));
    }

    // ---------------------------------------------------------------- NIVEL 2

    public Optional<BancoResumen> banco(int idBanco, LocalDate desde, LocalDate hasta) {
        MapSqlParameterSource p = fechas(desde, hasta).addValue("idBanco", idBanco);
        return jdbc.query(String.format(SQL_BANCOS, "AND b.id_banco = :idBanco"), p, MAP_BANCO)
                .stream().findFirst();
    }

    /** Productos que el banco tiene habilitados (tabla bancos_productos). */
    public List<String> productosHabilitados(int idBanco) {
        return jdbc.queryForList(
                "SELECT p.descripcion FROM bancos_productos bp"
              + " JOIN productos p ON p.id_producto = bp.id_producto"
              + " WHERE bp.id_banco = :id AND bp.habilitado = TRUE ORDER BY p.descripcion",
                new MapSqlParameterSource("id", idBanco), String.class);
    }

    public List<EvolucionDia> evolucion(int idBanco, LocalDate desde, LocalDate hasta) {
        String sql =
                "SELECT CAST(t.fecha_creacion AS date) AS dia, COUNT(*) AS total,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'APROBADA')  AS aprobadas,"
              + "  COUNT(*) FILTER (WHERE t.estado = 'RECHAZADA') AS rechazadas"
              + " FROM transferencias t WHERE t.id_banco = :idBanco" + FILTRO_FECHAS
              + " GROUP BY CAST(t.fecha_creacion AS date) ORDER BY dia";
        return jdbc.query(sql, fechas(desde, hasta).addValue("idBanco", idBanco),
                (rs, i) -> new EvolucionDia(rs.getDate("dia").toLocalDate(),
                        rs.getLong("total"), rs.getLong("aprobadas"), rs.getLong("rechazadas")));
    }

    public List<TransferenciaFila> transferencias(int idBanco, LocalDate desde, LocalDate hasta,
                                                  String estado, Integer idPais) {
        String sql =
                "SELECT t.id_transferencia, t.fecha_creacion, p.descripcion AS producto,"
              + "  pa.nombre AS pais, t.monto, t.moneda, t.estado"
              + " FROM transferencias t"
              + " JOIN productos p ON p.id_producto = t.id_producto"
              + " JOIN paises pa   ON pa.id_pais    = t.id_pais"
              + " WHERE t.id_banco = :idBanco" + FILTRO_FECHAS
              + " AND (CAST(:estado AS varchar) IS NULL OR t.estado = CAST(:estado AS varchar))"
              + " AND (CAST(:idPais AS integer) IS NULL OR t.id_pais = CAST(:idPais AS integer))"
              + " ORDER BY t.fecha_creacion DESC LIMIT 200";
        MapSqlParameterSource p = fechas(desde, hasta)
                .addValue("idBanco", idBanco)
                .addValue("estado", estado)
                .addValue("idPais", idPais);
        return jdbc.query(sql, p, (rs, i) -> new TransferenciaFila(
                rs.getLong("id_transferencia"), rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                rs.getString("producto"), rs.getString("pais"),
                rs.getBigDecimal("monto"), rs.getString("moneda"), rs.getString("estado")));
    }

    public List<Opcion> paises() {
        return jdbc.query("SELECT id_pais, nombre FROM paises ORDER BY nombre",
                (rs, i) -> new Opcion(rs.getInt("id_pais"), rs.getString("nombre")));
    }

    // ---------------------------------------------------------------- NIVEL 3

    public Optional<TransferenciaDetalle> detalle(long idTransferencia) {
        String sql =
                "SELECT t.id_transferencia, b.id_banco, b.nombre AS banco,"
              + "  p.codigo AS producto_codigo, p.descripcion AS producto,"
              + "  pa.nombre AS pais, pa.es_local, t.monto, t.moneda, t.estado,"
              + "  t.codigo_respuesta, cr.descripcion AS descripcion_respuesta, t.fecha_creacion"
              + " FROM transferencias t"
              + " JOIN bancos b    ON b.id_banco    = t.id_banco"
              + " JOIN productos p ON p.id_producto = t.id_producto"
              + " JOIN paises pa   ON pa.id_pais    = t.id_pais"
              + " LEFT JOIN codigos_respuesta cr ON cr.codigo = t.codigo_respuesta"
              + " WHERE t.id_transferencia = :id";
        return jdbc.query(sql, new MapSqlParameterSource("id", idTransferencia),
                (rs, i) -> new TransferenciaDetalle(
                        rs.getLong("id_transferencia"), rs.getInt("id_banco"), rs.getString("banco"),
                        rs.getString("producto_codigo"), rs.getString("producto"),
                        rs.getString("pais"), rs.getBoolean("es_local"),
                        rs.getBigDecimal("monto"), rs.getString("moneda"), rs.getString("estado"),
                        rs.getString("codigo_respuesta"), rs.getString("descripcion_respuesta"),
                        rs.getTimestamp("fecha_creacion").toLocalDateTime()))
                .stream().findFirst();
    }
}
