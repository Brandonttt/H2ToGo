package com.h2togo.backend.pedidos;

import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import com.h2togo.backend.direcciones.DireccionCliente;
import com.h2togo.backend.direcciones.DireccionClienteRepository;
import com.h2togo.backend.negocios.HorarioNegocio;
import com.h2togo.backend.negocios.HorarioNegocioRepository;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.pedidos.dto.PedidoCreateRequest;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedidos: creación (CU-004), cancelación (CU-005) e historiales (CU-007/013). La creación
 * valida cobertura (RN-001), horario (RN-004/RF-011), suspensión (RN-006) y, en abierta,
 * que exista purificadora que cumpla el precio (RN-028); captura snapshot de precios (RN-025/031).
 * El apartado de inventario y la validación de stock viven en F7 (RN-022, §10 #25).
 */
@Service
public class PedidoService {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Mexico_City");
    private static final int TAM_MAX_PAGINA = 100;

    private final PedidoRepository pedidoRepository;
    private final DireccionClienteRepository direccionRepository;
    private final NegocioRepository negocioRepository;
    private final HorarioNegocioRepository horarioRepository;
    private final ProductoNegocioRepository productoRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    public PedidoService(PedidoRepository pedidoRepository, DireccionClienteRepository direccionRepository,
            NegocioRepository negocioRepository, HorarioNegocioRepository horarioRepository,
            ProductoNegocioRepository productoRepository, NamedParameterJdbcTemplate jdbc) {
        this.pedidoRepository = pedidoRepository;
        this.direccionRepository = direccionRepository;
        this.negocioRepository = negocioRepository;
        this.horarioRepository = horarioRepository;
        this.productoRepository = productoRepository;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- CU-004: crear

    @Transactional
    public PedidoResponse crear(int idCliente, PedidoCreateRequest req) {
        // RN-006: cliente suspendido no puede pedir.
        Boolean suspendido = jdbc.queryForObject(
                "SELECT (suspendido_hasta IS NOT NULL AND suspendido_hasta > now()) FROM clientes WHERE id_usuario = :id",
                new MapSqlParameterSource("id", idCliente), Boolean.class);
        if (Boolean.TRUE.equals(suspendido)) {
            throw new BusinessRuleException("CLIENTE_SUSPENDIDO",
                    "La cuenta está suspendida temporalmente por ausencias (RN-006).");
        }

        // Dirección propia, activa y en zona de cobertura (RN-001).
        DireccionCliente dir = direccionRepository.findById(req.idDireccionEntrega())
                .filter(d -> d.getIdCliente().equals(idCliente) && d.isActivo())
                .orElseThrow(() -> new NotFoundException("DIRECCION_NO_ENCONTRADA", "Dirección no encontrada."));
        if (!dir.isEnZonaCobertura()) {
            throw new BusinessRuleException("FUERA_DE_COBERTURA",
                    "La dirección está fuera de la zona de cobertura (RN-001).");
        }

        boolean programado = req.programado() != null;
        if (programado && !req.programado().fechaProgramada().isAfter(OffsetDateTime.now())) {
            throw new BusinessRuleException("FECHA_PROGRAMADA_INVALIDA", "La fecha programada debe ser futura.");
        }

        Pedido pedido = (req.tipoSolicitud() == TipoSolicitudPedido.directa)
                ? construirDirecta(idCliente, req, programado)
                : construirAbierta(idCliente, req, programado);

        pedidoRepository.saveAndFlush(pedido);
        em.refresh(pedido); // cargar fecha_creacion / fecha_cambio generados por la BD
        return PedidoMapper.toResponse(pedido);
    }

    private Pedido construirDirecta(int idCliente, PedidoCreateRequest req, boolean programado) {
        if (req.idNegocio() == null) {
            throw new BusinessRuleException("NEGOCIO_REQUERIDO", "La modalidad directa requiere idNegocio.");
        }
        Negocio negocio = negocioRepository.findById(req.idNegocio())
                .filter(Negocio::isActivo)
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "El negocio no existe o está inactivo."));

        // Horario (RF-011/RN-004): abierto ahora (inmediato) o en la fecha programada.
        LocalDateTime momento = programado
                ? req.programado().fechaProgramada().atZoneSameInstant(ZONA_LOCAL).toLocalDateTime()
                : LocalDateTime.now(ZONA_LOCAL);
        if (!horarioAbierto(negocio.getId(), momento)) {
            throw new BusinessRuleException("NEGOCIO_CERRADO",
                    programado ? "La purificadora está cerrada en la fecha/hora programada."
                            : "La purificadora está cerrada; puedes programar el pedido en su horario.");
        }

        Pedido p = baseComun(idCliente, req, programado);
        p.setIdNegocioSolicitado(negocio.getId());

        BigDecimal total = BigDecimal.ZERO;
        for (var linea : req.detalles()) {
            ProductoNegocio prod = productoRepository
                    .findByIdNegocioAndIdMarca(negocio.getId(), linea.idMarca())
                    .filter(ProductoNegocio::isActivo)
                    .orElseThrow(() -> new BusinessRuleException("PRODUCTO_NO_DISPONIBLE",
                            "El negocio no vende la marca " + linea.idMarca() + "."));
            BigDecimal precio = prod.getPrecio();
            BigDecimal precioEnvase = linea.tieneEnvase() ? null : prod.getPrecioEnvase(); // RN-025/031
            DetallePedido d = detalle(linea.idMarca(), linea.cantidad(), linea.tieneEnvase(), precio, precioEnvase);
            p.agregarDetalle(d);
            BigDecimal envase = linea.tieneEnvase() ? BigDecimal.ZERO : prod.getPrecioEnvase();
            total = total.add(precio.add(envase).multiply(BigDecimal.valueOf(linea.cantidad())));
        }
        p.setTotalPagar(total);
        return p;
    }

    private Pedido construirAbierta(int idCliente, PedidoCreateRequest req, boolean programado) {
        if (req.precioMaximoGarrafon() == null) {
            throw new BusinessRuleException("PRECIO_MAXIMO_REQUERIDO",
                    "La modalidad abierta requiere precioMaximoGarrafon.");
        }
        // Una línea por marca en abierta (no mezclar con/sin envase de la misma marca).
        Set<Integer> marcas = req.detalles().stream().map(l -> l.idMarca()).collect(Collectors.toSet());
        if (marcas.size() != req.detalles().size()) {
            throw new BusinessRuleException("MARCA_DUPLICADA", "En modalidad abierta cada marca va una sola vez.");
        }
        // RN-028: debe existir al menos una purificadora que cumpla el precio en TODAS las marcas.
        if (!existePurificadoraQueCumple(marcas, req.precioMaximoGarrafon())) {
            throw new BusinessRuleException("SIN_PURIFICADORAS",
                    "Ninguna purificadora cumple el precio máximo para todas las marcas (RN-028).");
        }

        Pedido p = baseComun(idCliente, req, programado);
        p.setPrecioMaximoGarrafon(req.precioMaximoGarrafon());

        BigDecimal total = BigDecimal.ZERO;
        for (var linea : req.detalles()) {
            // Precio estimado (§10 #27): el definitivo se fija al aceptar/cerrar (F7).
            BigDecimal precioEnvase = linea.tieneEnvase() ? null : BigDecimal.ZERO;
            DetallePedido d = detalle(linea.idMarca(), linea.cantidad(), linea.tieneEnvase(),
                    req.precioMaximoGarrafon(), precioEnvase);
            p.agregarDetalle(d);
            total = total.add(req.precioMaximoGarrafon().multiply(BigDecimal.valueOf(linea.cantidad())));
        }
        p.setTotalPagar(total);
        return p;
    }

    private Pedido baseComun(int idCliente, PedidoCreateRequest req, boolean programado) {
        Pedido p = new Pedido();
        p.setIdCliente(idCliente);
        p.setIdDireccionEntrega(req.idDireccionEntrega());
        p.setTipoSolicitud(req.tipoSolicitud());
        p.setIndicacionesEntrega(req.indicaciones());
        p.setGarrafonesTotales(req.detalles().stream().mapToInt(l -> l.cantidad()).sum());
        EstadoPedido estado = programado ? EstadoPedido.pendiente_programado : EstadoPedido.pendiente;
        p.setEstadoActual(estado);
        if (programado) {
            p.setEsProgramado(true);
            p.setFechaProgramada(req.programado().fechaProgramada());
        }
        HistorialEstadoPedido h = new HistorialEstadoPedido();
        h.setEstado(estado);
        p.agregarHistorial(h);
        return p;
    }

    private static DetallePedido detalle(int idMarca, int cantidad, boolean tieneEnvase,
            BigDecimal precio, BigDecimal precioEnvase) {
        DetallePedido d = new DetallePedido();
        d.setIdMarca(idMarca);
        d.setCantidadSolicitada(cantidad);
        d.setTieneEnvase(tieneEnvase);
        d.setPrecioUnitario(precio);
        d.setPrecioEnvaseUnitario(precioEnvase);
        return d;
    }

    // ---------------------------------------------------------------- CU-005: cancelar

    @Transactional
    public PedidoResponse cancelar(int idCliente, int idPedido, String motivo) {
        Pedido p = pedidoRepository.findById(idPedido)
                .filter(pe -> pe.getIdCliente().equals(idCliente))
                .orElseThrow(() -> new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado."));
        // CU-005: solo cancelable en pendiente / pendiente_programado (§10 #26).
        if (p.getEstadoActual() != EstadoPedido.pendiente
                && p.getEstadoActual() != EstadoPedido.pendiente_programado) {
            throw new BusinessRuleException("PEDIDO_NO_CANCELABLE",
                    "El pedido ya no puede cancelarse en estado " + p.getEstadoActual() + ".");
        }
        liberarApartados(idPedido); // RN-023 (no-op si no hay apartados; los crea F7)
        p.setEstadoActual(EstadoPedido.cancelado);
        HistorialEstadoPedido h = new HistorialEstadoPedido();
        h.setEstado(EstadoPedido.cancelado);
        h.setNotasAdicionales(motivo);
        p.agregarHistorial(h);
        pedidoRepository.saveAndFlush(p);
        em.refresh(p);
        return PedidoMapper.toResponse(p);
    }

    /** Libera los apartados del pedido restaurando la cantidad reservada (RN-023). */
    private void liberarApartados(int idPedido) {
        var params = new MapSqlParameterSource("pedido", idPedido);
        jdbc.update("""
                UPDATE lotes_inventario l SET cantidad_apartada = l.cantidad_apartada - a.cantidad
                FROM apartados_pedido a
                WHERE a.id_pedido = :pedido AND a.id_lote_base = l.id_lote""", params);
        jdbc.update("""
                UPDATE inventario_vehiculo iv SET cantidad_apartada = iv.cantidad_apartada - a.cantidad
                FROM apartados_pedido a
                WHERE a.id_pedido = :pedido AND a.id_inventario_vehiculo = iv.id_inventario_vehiculo""", params);
        jdbc.update("DELETE FROM apartados_pedido WHERE id_pedido = :pedido", params);
    }

    // ---------------------------------------------------------------- Consultas

    @Transactional(readOnly = true)
    public PedidoResponse detalle(int idPedido) {
        Pedido p = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado."));
        exigirVisibilidad(p); // RN-016
        return PedidoMapper.toResponse(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PedidoResumen> misPedidos(int idCliente, Pageable pageable) {
        Page<PedidoResumen> page = pedidoRepository
                .findByIdClienteOrderByFechaCreacionDesc(idCliente, acotar(pageable))
                .map(PedidoMapper::toResumen);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PedidoResumen> misEntregas(int idRepartidor, Pageable pageable) {
        Page<PedidoResumen> page = pedidoRepository
                .findByIdRepartidorOrderByFechaCreacionDesc(idRepartidor, acotar(pageable))
                .map(PedidoMapper::toResumen);
        return PagedResponse.of(page);
    }

    // ---------------------------------------------------------------- Helpers

    /** RN-016: cliente dueño, repartidor asignado o administrador. */
    private void exigirVisibilidad(Pedido p) {
        UsuarioPrincipal actor = SecurityUtils.actual();
        boolean permitido = switch (actor.rol()) {
            case admin -> true;
            case cliente -> actor.id().equals(p.getIdCliente());
            case repartidor -> actor.id().equals(p.getIdRepartidor());
        };
        if (!permitido) {
            throw new AccessDeniedException("No tiene permiso para ver este pedido (RN-016).");
        }
    }

    private boolean horarioAbierto(int idNegocio, LocalDateTime momento) {
        short dow = (short) momento.getDayOfWeek().getValue(); // 1=lunes … 7=domingo
        LocalTime hora = momento.toLocalTime();
        return horarioRepository.findByIdNegocioOrderByDiaSemana(idNegocio).stream()
                .filter(h -> h.getDiaSemana() == dow && !h.isCerrado()
                        && h.getHoraApertura() != null && h.getHoraCierre() != null)
                .anyMatch(h -> !hora.isBefore(h.getHoraApertura()) && hora.isBefore(h.getHoraCierre()));
    }

    private boolean existePurificadoraQueCumple(Set<Integer> marcas, BigDecimal precioMax) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM negocios n WHERE n.activo AND (
                    SELECT COUNT(DISTINCT pn.id_marca) FROM productos_negocio pn
                    WHERE pn.id_negocio = n.id_negocio AND pn.activo
                      AND pn.precio <= :max AND pn.id_marca IN (:marcas)
                ) = :num""",
                new MapSqlParameterSource().addValue("max", precioMax)
                        .addValue("marcas", marcas).addValue("num", marcas.size()),
                Integer.class);
        return count != null && count > 0;
    }

    private static Pageable acotar(Pageable pageable) {
        if (pageable.getPageSize() > TAM_MAX_PAGINA) {
            return PageRequest.of(pageable.getPageNumber(), TAM_MAX_PAGINA, pageable.getSort());
        }
        return pageable;
    }
}
