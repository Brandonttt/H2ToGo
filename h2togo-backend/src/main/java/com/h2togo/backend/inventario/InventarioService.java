package com.h2togo.backend.inventario;

import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.inventario.dto.CargaItem;
import com.h2togo.backend.inventario.dto.InventarioBaseResponse;
import com.h2togo.backend.inventario.dto.InventarioItem;
import com.h2togo.backend.inventario.dto.InventarioVehiculoResponse;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.inventario.dto.LoteResponse;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.repartidores.dto.JornadaResponse;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventario por lotes y jornada del repartidor (CU-018/019/008). Todo lo que mueve
 * existencias es SQL nativo y transaccional: la carga usa {@code FOR UPDATE} + FIFO por
 * caducidad (RF-022/RNF-014, §10 #24). Cada traspaso/devolución es UNA fila de movimiento
 * con ambas ubicaciones (CHECK de v6).
 */
@Service
public class InventarioService {

    private final NamedParameterJdbcTemplate jdbc;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;
    private final ProductoNegocioRepository productoRepository;
    private final VehiculoNegocioRepository vehiculoRepository;

    public InventarioService(NamedParameterJdbcTemplate jdbc, RepartidorRepository repartidorRepository,
            NegocioRepository negocioRepository, ProductoNegocioRepository productoRepository,
            VehiculoNegocioRepository vehiculoRepository) {
        this.jdbc = jdbc;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
        this.productoRepository = productoRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    // ---------------------------------------------------------------- CU-018: entrada a base

    @Transactional
    public LoteResponse registrarLote(int idRepartidor, LoteEntradaRequest req) {
        Negocio negocio = negocioComoDueno(idRepartidor); // solo dueño (RN-021)
        int idNegocio = negocio.getId();

        ProductoNegocio prod = productoRepository.findByIdNegocioAndIdMarca(idNegocio, req.idMarca())
                .orElseThrow(() -> new BusinessRuleException("PRODUCTO_NO_CONFIGURADO",
                        "La marca no está configurada en el catálogo del negocio."));

        // RN-030: caducidad estrictamente posterior a hoy + 7 días.
        if (!req.fechaCaducidad().isAfter(LocalDate.now().plusDays(7))) {
            throw new BusinessRuleException("RN-030_CADUCIDAD_INVALIDA",
                    "La fecha de caducidad debe ser posterior a hoy + 7 días.");
        }

        int stockBase = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad_actual), 0) FROM lotes_inventario
                WHERE id_negocio = :neg AND id_marca = :marca AND activo""",
                new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", req.idMarca()),
                Integer.class);
        if (stockBase + req.cantidad() > prod.getCapacidadMaxima()) {
            throw new BusinessRuleException("CAPACIDAD_BASE_EXCEDIDA",
                    "La cantidad excede la capacidad máxima de la base para la marca.");
        }

        var params = new MapSqlParameterSource()
                .addValue("neg", idNegocio).addValue("marca", req.idMarca())
                .addValue("cad", req.fechaCaducidad()).addValue("cant", req.cantidad())
                .addValue("costo", req.costoUnitario()).addValue("prov", req.proveedor())
                .addValue("notas", req.notas()).addValue("rep", idRepartidor);
        Integer idLote = jdbc.queryForObject("""
                INSERT INTO lotes_inventario
                    (id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual,
                     cantidad_apartada, costo_unitario, proveedor, activo)
                VALUES (:neg, :marca, :cad, :cant, :cant, 0, :costo, :prov, TRUE)
                RETURNING id_lote""", params, Integer.class);

        params.addValue("idLote", idLote);
        jdbc.update("""
                INSERT INTO movimientos_inventario
                    (id_lote_base, tipo, cantidad, id_repartidor_responsable, costo_unitario, proveedor, notas)
                VALUES (:idLote, 'entrada_proveedor'::tipo_movimiento, :cant, :rep, :costo, :prov, :notas)""",
                params);

        return new LoteResponse(idLote, req.idMarca(), marcaNombre(req.idMarca()),
                req.fechaCaducidad(), req.cantidad());
    }

    // ---------------------------------------------------------------- Consultas

    @Transactional(readOnly = true)
    public InventarioBaseResponse inventarioBase(int idRepartidor) {
        int idNegocio = negocioDelRepartidor(idRepartidor).getId();
        List<InventarioItem> lotes = jdbc.query("""
                SELECT l.id_lote, l.id_marca, m.nombre AS marca, l.fecha_caducidad,
                       l.cantidad_actual, l.cantidad_apartada
                FROM lotes_inventario l JOIN marcas m ON m.id_marca = l.id_marca
                WHERE l.id_negocio = :neg AND l.activo
                ORDER BY l.fecha_caducidad, l.id_lote""",
                new MapSqlParameterSource("neg", idNegocio), InventarioService::mapItem);
        return new InventarioBaseResponse(lotes);
    }

    @Transactional(readOnly = true)
    public InventarioVehiculoResponse inventarioVehiculo(int idRepartidor) {
        Repartidor r = repartidor(idRepartidor);
        Integer idVehiculo = r.getIdVehiculoActual();
        if (idVehiculo == null) {
            throw new BusinessRuleException("SIN_JORNADA", "El repartidor no tiene una jornada activa.");
        }
        int capacidad = vehiculo(idVehiculo).getCapacidadGarrafones();
        List<InventarioItem> lotes = jdbc.query("""
                SELECT iv.id_lote, l.id_marca, m.nombre AS marca, l.fecha_caducidad,
                       iv.cantidad_actual, iv.cantidad_apartada
                FROM inventario_vehiculo iv
                JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                JOIN marcas m ON m.id_marca = l.id_marca
                WHERE iv.id_vehiculo = :veh AND iv.activo AND iv.cantidad_actual > 0
                ORDER BY l.fecha_caducidad, l.id_lote""",
                new MapSqlParameterSource("veh", idVehiculo), InventarioService::mapItem);
        int ocupado = lotes.stream().mapToInt(InventarioItem::cantidadActual).sum();
        return new InventarioVehiculoResponse(idVehiculo, capacidad, ocupado, capacidad - ocupado, lotes);
    }

    // ---------------------------------------------------------------- CU-019: carga FIFO

    @Transactional
    public InventarioVehiculoResponse cargarVehiculo(int idRepartidor, List<CargaItem> cargas) {
        Repartidor r = repartidor(idRepartidor);
        if (r.getIdVehiculoActual() == null) {
            throw new BusinessRuleException("SIN_JORNADA",
                    "Debe iniciar jornada (seleccionar vehículo) antes de cargar.");
        }
        ejecutarCarga(r, cargas);
        return inventarioVehiculo(idRepartidor);
    }

    // ---------------------------------------------------------------- CU-008: iniciar jornada

    @Transactional
    public JornadaResponse iniciarJornada(int idRepartidor, IniciarJornadaRequest req) {
        Repartidor r = repartidor(idRepartidor);
        VehiculoNegocio veh = vehiculo(req.idVehiculo());
        // RN-014: el vehículo debe ser del propio negocio y estar activo.
        if (!veh.getIdNegocio().equals(r.getIdNegocio()) || !veh.isActivo()) {
            throw new BusinessRuleException("VEHICULO_INVALIDO",
                    "El vehículo no pertenece a tu negocio o está inactivo.");
        }
        r.setIdVehiculoActual(req.idVehiculo());
        repartidorRepository.saveAndFlush(r); // que las queries nativas vean el vehículo actual

        if (req.cargaInicial() != null && !req.cargaInicial().isEmpty()) {
            ejecutarCarga(r, req.cargaInicial());
        }
        r.setEstadoOperativo(true); // RN-007: disponible tras cargar
        repartidorRepository.save(r);

        return new JornadaResponse(req.idVehiculo(), true, inventarioVehiculo(idRepartidor));
    }

    // ---------------------------------------------------------------- Devolución / fin de jornada

    @Transactional
    public void devolucion(int idRepartidor) {
        Repartidor r = repartidor(idRepartidor);
        Integer idVehiculo = r.getIdVehiculoActual();
        if (idVehiculo == null) {
            throw new BusinessRuleException("SIN_JORNADA", "No hay jornada activa que cerrar.");
        }
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT id_inventario_vehiculo, id_lote, cantidad_actual
                FROM inventario_vehiculo WHERE id_vehiculo = :veh AND cantidad_actual > 0
                FOR UPDATE""", new MapSqlParameterSource("veh", idVehiculo));
        for (Map<String, Object> f : filas) {
            var p = new MapSqlParameterSource()
                    .addValue("iv", f.get("id_inventario_vehiculo"))
                    .addValue("lote", f.get("id_lote"))
                    .addValue("cant", f.get("cantidad_actual"))
                    .addValue("rep", idRepartidor);
            jdbc.update("UPDATE lotes_inventario SET cantidad_actual = cantidad_actual + :cant WHERE id_lote = :lote", p);
            jdbc.update("UPDATE inventario_vehiculo SET cantidad_actual = 0 WHERE id_inventario_vehiculo = :iv", p);
            jdbc.update("""
                    INSERT INTO movimientos_inventario
                        (id_lote_base, id_inventario_vehiculo, tipo, cantidad, id_repartidor_responsable)
                    VALUES (:lote, :iv, 'devolucion_a_base'::tipo_movimiento, :cant, :rep)""", p);
        }
        r.setEstadoOperativo(false);
        r.setIdVehiculoActual(null);
        repartidorRepository.save(r);
    }

    // ---------------------------------------------------------------- Núcleo FIFO

    private void ejecutarCarga(Repartidor r, List<CargaItem> cargas) {
        int idVehiculo = r.getIdVehiculoActual();
        int idNegocio = r.getIdNegocio();
        int capacidad = vehiculo(idVehiculo).getCapacidadGarrafones();

        int ocupado = jdbc.queryForObject(
                "SELECT COALESCE(SUM(cantidad_actual), 0) FROM inventario_vehiculo WHERE id_vehiculo = :veh AND activo",
                new MapSqlParameterSource("veh", idVehiculo), Integer.class);
        int totalCargar = cargas.stream().mapToInt(CargaItem::cantidad).sum();
        if (ocupado + totalCargar > capacidad) { // RN-008
            throw new BusinessRuleException("CAPACIDAD_VEHICULO_EXCEDIDA",
                    "La carga supera la capacidad del vehículo (" + capacidad + ").");
        }
        for (CargaItem item : cargas) {
            cargarMarcaFifo(idNegocio, idVehiculo, item.idMarca(), item.cantidad(), r.getId());
        }
    }

    private void cargarMarcaFifo(int idNegocio, int idVehiculo, int idMarca, int cantidad, int idRepartidor) {
        // FOR UPDATE bloquea los lotes vigentes mientras se descuentan (concurrencia RF-022).
        List<Map<String, Object>> lotes = jdbc.queryForList("""
                SELECT id_lote, (cantidad_actual - cantidad_apartada) AS disponible
                FROM lotes_inventario
                WHERE id_negocio = :neg AND id_marca = :marca AND activo
                  AND fecha_caducidad >= CURRENT_DATE AND (cantidad_actual - cantidad_apartada) > 0
                ORDER BY fecha_caducidad, id_lote
                FOR UPDATE""",
                new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", idMarca));

        int disponibleTotal = lotes.stream().mapToInt(l -> ((Number) l.get("disponible")).intValue()).sum();
        if (disponibleTotal < cantidad) {
            throw new BusinessRuleException("STOCK_INSUFICIENTE",
                    "Stock vigente insuficiente en la base para la marca " + idMarca + ".");
        }

        int restante = cantidad;
        for (Map<String, Object> lote : lotes) {
            if (restante == 0) {
                break;
            }
            int disponible = ((Number) lote.get("disponible")).intValue();
            int tomar = Math.min(restante, disponible);
            var p = new MapSqlParameterSource()
                    .addValue("lote", lote.get("id_lote")).addValue("veh", idVehiculo)
                    .addValue("tomar", tomar).addValue("rep", idRepartidor);

            jdbc.update("UPDATE lotes_inventario SET cantidad_actual = cantidad_actual - :tomar WHERE id_lote = :lote", p);
            Integer idIv = jdbc.queryForObject("""
                    INSERT INTO inventario_vehiculo (id_vehiculo, id_lote, cantidad_actual, cantidad_apartada, activo)
                    VALUES (:veh, :lote, :tomar, 0, TRUE)
                    ON CONFLICT (id_vehiculo, id_lote)
                    DO UPDATE SET cantidad_actual = inventario_vehiculo.cantidad_actual + :tomar, activo = TRUE
                    RETURNING id_inventario_vehiculo""", p, Integer.class);
            p.addValue("iv", idIv);
            jdbc.update("""
                    INSERT INTO movimientos_inventario
                        (id_lote_base, id_inventario_vehiculo, tipo, cantidad, id_repartidor_responsable)
                    VALUES (:lote, :iv, 'traspaso_a_vehiculo'::tipo_movimiento, :tomar, :rep)""", p);
            restante -= tomar;
        }
    }

    // ---------------------------------------------------------------- Helpers

    private Repartidor repartidor(int idRepartidor) {
        return repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "El usuario no es un repartidor."));
    }

    private Negocio negocioDelRepartidor(int idRepartidor) {
        return negocioRepository.findById(repartidor(idRepartidor).getIdNegocio())
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "El repartidor no tiene negocio."));
    }

    private Negocio negocioComoDueno(int idRepartidor) {
        Negocio negocio = negocioDelRepartidor(idRepartidor);
        if (!Integer.valueOf(idRepartidor).equals(negocio.getIdDueno())) {
            throw new AccessDeniedException("Solo el dueño del negocio puede registrar inventario (RN-021).");
        }
        return negocio;
    }

    private VehiculoNegocio vehiculo(int idVehiculo) {
        return vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new NotFoundException("VEHICULO_NO_ENCONTRADO", "Vehículo no encontrado."));
    }

    private String marcaNombre(int idMarca) {
        return jdbc.queryForObject("SELECT nombre FROM marcas WHERE id_marca = :m",
                new MapSqlParameterSource("m", idMarca), String.class);
    }

    private static InventarioItem mapItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new InventarioItem(
                rs.getInt("id_lote"), rs.getInt("id_marca"), rs.getString("marca"),
                rs.getObject("fecha_caducidad", LocalDate.class),
                rs.getInt("cantidad_actual"), rs.getInt("cantidad_apartada"));
    }
}
