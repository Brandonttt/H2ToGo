package com.h2togo.backend.negocios;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.negocios.dto.NegocioCercanoResponse;
import com.h2togo.backend.negocios.dto.PerfilNegocioResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública (autenticada) de purificadoras: perfil (CU-022) y listado por cercanía
 * (apoya CU-004). Cualquier usuario autenticado; solo datos públicos (RN-016).
 */
@RestController
@RequestMapping("/api/v1/negocios")
public class NegociosPublicController {

    private final NegocioService negocioService;

    public NegociosPublicController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @GetMapping("/{id}/perfil")
    public PerfilNegocioResponse perfil(@PathVariable int id) {
        return negocioService.perfil(id, true);
    }

    /** {@code ?cerca=lat,lon&limite=n}: negocios activos en cobertura, ordenados por distancia. */
    @GetMapping
    public List<NegocioCercanoResponse> cercanos(
            @RequestParam String cerca,
            @RequestParam(defaultValue = "5") int limite) {
        double[] coords = parseCerca(cerca);
        int lim = Math.max(1, Math.min(limite, 50));
        return negocioService.cercanos(coords[0], coords[1], lim);
    }

    private static double[] parseCerca(String cerca) {
        String[] partes = cerca.split(",");
        if (partes.length != 2) {
            throw new BusinessRuleException("CERCA_INVALIDO", "El parámetro 'cerca' debe ser 'lat,lon'.");
        }
        try {
            return new double[]{Double.parseDouble(partes[0].trim()), Double.parseDouble(partes[1].trim())};
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("CERCA_INVALIDO", "Coordenadas inválidas en 'cerca'.");
        }
    }
}
