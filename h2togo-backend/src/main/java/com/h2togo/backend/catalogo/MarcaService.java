package com.h2togo.backend.catalogo;

import com.h2togo.backend.catalogo.dto.MarcaResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo global de marcas. */
@Service
public class MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaService(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    @Transactional(readOnly = true)
    public List<MarcaResponse> listar() {
        return marcaRepository.findByActivoTrue().stream()
                .map(m -> new MarcaResponse(m.getId(), m.getNombre()))
                .toList();
    }
}
