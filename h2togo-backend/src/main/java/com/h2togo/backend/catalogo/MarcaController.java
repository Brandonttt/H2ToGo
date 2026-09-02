package com.h2togo.backend.catalogo;

import com.h2togo.backend.catalogo.dto.MarcaResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo global de marcas (C/R/A autenticado). */
@RestController
@RequestMapping("/api/v1/marcas")
public class MarcaController {

    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping
    public List<MarcaResponse> listar() {
        return marcaService.listar();
    }
}
