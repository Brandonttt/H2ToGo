package com.h2togo.backend.negocios;

import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.catalogo.dto.PrecioRequest;
import com.h2togo.backend.catalogo.dto.ProductoResponse;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.negocios.dto.HorarioRequest;
import com.h2togo.backend.negocios.dto.HorarioResponse;
import com.h2togo.backend.negocios.dto.NegocioCercanoResponse;
import com.h2togo.backend.negocios.dto.PerfilNegocioResponse;
import com.h2togo.backend.negocios.dto.PerfilNegocioResponse.DireccionBase;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Negocios (purificadoras), su catálogo y horarios (CU-022, CU-023). Lo espacial
 * (ubicación de la base, KNN de cercanía, cobertura) va por SQL nativo (§1). Las
 * ediciones (horario, precio) son directas y solo del dueño (RN-021).
 */
@Service
public class NegocioService {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Mexico_City");

    private final NamedParameterJdbcTemplate jdbc;
    private final NegocioRepository negocioRepository;
    private final HorarioNegocioRepository horarioRepository;
    private final ProductoNegocioRepository productoRepository;
    private final RepartidorRepository repartidorRepository;

    public NegocioService(NamedParameterJdbcTemplate jdbc, NegocioRepository negocioRepository,
            HorarioNegocioRepository horarioRepository, ProductoNegocioRepository productoRepository,
            RepartidorRepository repartidorRepository) {
        this.jdbc = jdbc;
        this.negocioRepository = negocioRepository;
        this.horarioRepository = horarioRepository;
        this.productoRepository = productoRepository;
        this.repartidorRepository = repartidorRepository;
    }

    // ---------------------------------------------------------------- Perfil / listados

    @Transactional(readOnly = true)
    public PerfilNegocioResponse perfil(int idNegocio, boolean soloActivo) {
        var params = new MapSqlParameterSource("id", idNegocio);
        String filtroActivo = soloActivo ? " AND activo" : "";
        List<PerfilNegocioResponse> base = jdbc.query("""
                SELECT id_negocio, nombre_comercial, activo, calle, numero_exterior, numero_interior,
                       colonia, codigo_postal, referencias,
                       ST_Y(ubicacion_base::geometry) AS lat, ST_X(ubicacion_base::geometry) AS lon
                FROM negocios WHERE id_negocio = :id""" + filtroActivo,
                params, NegocioService::mapPerfilBase);
        if (base.isEmpty()) {
            throw new NotFoundException("NEGOCIO_NO_ENCONTRADO", "No existe el negocio " + idNegocio + ".");
        }
        PerfilNegocioResponse esqueleto = base.get(0);

        List<HorarioResponse> horarios = horarioRepository.findByIdNegocioOrderByDiaSemana(idNegocio)
                .stream()
                .map(h -> new HorarioResponse(h.getDiaSemana(), h.getHoraApertura(), h.getHoraCierre(), h.isCerrado()))
                .toList();
        List<ProductoResponse> productos = productosDeNegocio(idNegocio, null);

        return new PerfilNegocioResponse(esqueleto.id(), esqueleto.nombreComercial(), esqueleto.activo(),
                esqueleto.direccion(), abiertoAhora(horarios), horarios, productos);
    }

    @Transactional(readOnly = true)
    public List<NegocioCercanoResponse> cercanos(double lat, double lon, int limite) {
        var params = new MapSqlParameterSource()
                .addValue("lat", lat).addValue("lon", lon).addValue("limite", limite);
        return jdbc.query("""
                SELECT n.id_negocio, n.nombre_comercial,
                       ST_Y(n.ubicacion_base::geometry) AS lat, ST_X(n.ubicacion_base::geometry) AS lon,
                       ST_Distance(n.ubicacion_base, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS dist
                FROM negocios n
                WHERE n.activo AND n.ubicacion_base IS NOT NULL
                  AND EXISTS (SELECT 1 FROM zonas_cobertura z WHERE z.activo AND ST_Covers(z.geom, n.ubicacion_base))
                ORDER BY n.ubicacion_base <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                LIMIT :limite
                """, params, (rs, n) -> new NegocioCercanoResponse(
                        rs.getInt("id_negocio"), rs.getString("nombre_comercial"),
                        rs.getDouble("lat"), rs.getDouble("lon"), rs.getDouble("dist")));
    }

    @Transactional(readOnly = true)
    public PerfilNegocioResponse miNegocio(int idRepartidor) {
        return perfil(negocioDelRepartidor(idRepartidor).getId(), false);
    }

    // ---------------------------------------------------------------- Ediciones del dueño

    @Transactional
    public List<HorarioResponse> actualizarHorarios(int idRepartidor, List<HorarioRequest> dias) {
        Negocio negocio = negocioComoDueno(idRepartidor);
        validarHorarios(dias);

        horarioRepository.deleteByIdNegocio(negocio.getId());
        horarioRepository.flush();
        List<HorarioNegocio> nuevos = new ArrayList<>();
        for (HorarioRequest d : dias) {
            HorarioNegocio h = new HorarioNegocio();
            h.setIdNegocio(negocio.getId());
            h.setDiaSemana(d.diaSemana());
            h.setCerrado(d.cerrado());
            h.setHoraApertura(d.horaApertura());
            h.setHoraCierre(d.horaCierre());
            nuevos.add(h);
        }
        horarioRepository.saveAll(nuevos);
        return horarioRepository.findByIdNegocioOrderByDiaSemana(negocio.getId()).stream()
                .map(h -> new HorarioResponse(h.getDiaSemana(), h.getHoraApertura(), h.getHoraCierre(), h.isCerrado()))
                .toList();
    }

    @Transactional
    public ProductoResponse actualizarPrecio(int idRepartidor, int idProducto, PrecioRequest req) {
        ProductoNegocio producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("PRODUCTO_NO_ENCONTRADO",
                        "No existe el producto " + idProducto + "."));
        exigirDueno(idRepartidor, producto.getIdNegocio());

        producto.setPrecio(req.precio());
        producto.setPrecioEnvase(req.precioEnvase());
        // saveAndFlush: la respuesta se arma con una query NATIVA, que no ve el cambio JPA
        // hasta que se sincroniza a la BD. RF-028: cambio inmediato (RN-025 no afecta pedidos previos).
        productoRepository.saveAndFlush(producto);

        return productosDeNegocio(producto.getIdNegocio(), idProducto).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("PRODUCTO_NO_ENCONTRADO", "Producto no disponible."));
    }

    // ---------------------------------------------------------------- Helpers

    private List<ProductoResponse> productosDeNegocio(int idNegocio, Integer idProducto) {
        var params = new MapSqlParameterSource("id", idNegocio).addValue("idProd", idProducto);
        String filtroProd = idProducto == null ? "" : " AND pn.id_producto_negocio = :idProd";
        String sql = """
                SELECT pn.id_producto_negocio, pn.id_marca, m.nombre AS marca, pn.precio, pn.precio_envase,
                       pn.capacidad_maxima, pn.activo,
                       COALESCE((SELECT SUM(l.cantidad_actual - l.cantidad_apartada)
                                 FROM lotes_inventario l
                                 WHERE l.id_negocio = pn.id_negocio AND l.id_marca = pn.id_marca AND l.activo), 0) AS stock
                FROM productos_negocio pn
                JOIN marcas m ON m.id_marca = pn.id_marca
                WHERE pn.id_negocio = :id AND pn.activo
                """ + filtroProd + " ORDER BY m.nombre";
        return jdbc.query(sql,
                params, (rs, n) -> new ProductoResponse(
                        rs.getInt("id_producto_negocio"), rs.getInt("id_marca"), rs.getString("marca"),
                        rs.getBigDecimal("precio"), rs.getBigDecimal("precio_envase"),
                        rs.getInt("capacidad_maxima"), rs.getBoolean("activo"), rs.getLong("stock")));
    }

    private Negocio negocioDelRepartidor(int idRepartidor) {
        Repartidor r = repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO",
                        "El usuario no es un repartidor."));
        return negocioRepository.findById(r.getIdNegocio())
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO",
                        "El repartidor no tiene un negocio asociado."));
    }

    /** Verifica que el repartidor sea el dueño de SU negocio (RN-021). */
    private Negocio negocioComoDueno(int idRepartidor) {
        Negocio negocio = negocioDelRepartidor(idRepartidor);
        if (!Integer.valueOf(idRepartidor).equals(negocio.getIdDueno())) {
            throw new AccessDeniedException("Solo el dueño del negocio puede realizar este cambio (RN-021).");
        }
        return negocio;
    }

    /** Verifica que el repartidor sea el dueño del negocio indicado (RN-021). */
    private void exigirDueno(int idRepartidor, Integer idNegocio) {
        Negocio negocio = negocioRepository.findById(idNegocio)
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "Negocio no encontrado."));
        if (!Integer.valueOf(idRepartidor).equals(negocio.getIdDueno())) {
            throw new AccessDeniedException("Solo el dueño del negocio puede realizar este cambio (RN-021).");
        }
    }

    private static void validarHorarios(List<HorarioRequest> dias) {
        if (dias == null || dias.size() != 7) {
            throw new BusinessRuleException("HORARIO_INVALIDO", "Debe enviar exactamente los 7 días de la semana.");
        }
        boolean[] vistos = new boolean[8];
        for (HorarioRequest d : dias) {
            if (d.diaSemana() == null || d.cerrado() == null) {
                throw new BusinessRuleException("HORARIO_INVALIDO",
                        "Cada día requiere 'diaSemana' y 'cerrado'.");
            }
            int dow = d.diaSemana();
            if (dow < 1 || dow > 7 || vistos[dow]) {
                throw new BusinessRuleException("HORARIO_INVALIDO",
                        "Los días deben ser 1..7 sin repetir (1=lunes, 7=domingo).");
            }
            vistos[dow] = true;
            if (d.cerrado()) {
                if (d.horaApertura() != null || d.horaCierre() != null) {
                    throw new BusinessRuleException("HORARIO_INVALIDO",
                            "Un día cerrado no debe llevar horas.");
                }
            } else {
                if (d.horaApertura() == null || d.horaCierre() == null
                        || !d.horaApertura().isBefore(d.horaCierre())) {
                    throw new BusinessRuleException("HORARIO_INVALIDO",
                            "Un día abierto requiere apertura y cierre, con apertura anterior al cierre.");
                }
            }
        }
    }

    /** RN-004: abierto/cerrado según el horario del día y la hora local (America/Mexico_City). */
    private static boolean abiertoAhora(List<HorarioResponse> horarios) {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LOCAL);
        short dow = (short) ahora.getDayOfWeek().getValue(); // 1=lunes … 7=domingo
        LocalTime hora = ahora.toLocalTime();
        return horarios.stream()
                .filter(h -> h.diaSemana() == dow && !h.cerrado()
                        && h.horaApertura() != null && h.horaCierre() != null)
                .anyMatch(h -> !hora.isBefore(h.horaApertura()) && hora.isBefore(h.horaCierre()));
    }

    private static PerfilNegocioResponse mapPerfilBase(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Double lat = (Double) rs.getObject("lat");
        Double lon = (Double) rs.getObject("lon");
        DireccionBase direccion = (lat == null || lon == null) ? null : new DireccionBase(
                rs.getString("calle"), rs.getString("numero_exterior"), rs.getString("numero_interior"),
                rs.getString("colonia"), rs.getString("codigo_postal"), rs.getString("referencias"), lat, lon);
        return new PerfilNegocioResponse(rs.getInt("id_negocio"), rs.getString("nombre_comercial"),
                rs.getBoolean("activo"), direccion, false, List.of(), List.of());
    }
}
