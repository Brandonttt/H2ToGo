package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.AltaMarcaRequest;
import com.h2togo.backend.admin.dto.EstadoMarcaRequest;
import com.h2togo.backend.admin.dto.MarcaAdminFila;
import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión del catálogo global de marcas de agua por el administrador.
 * Rol ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/marcas")
public class AdminMarcasController {

    private final MarcaRepository marcaRepository;

    public AdminMarcasController(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    @GetMapping
    public List<MarcaAdminFila> listar() {
        return marcaRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(m -> new MarcaAdminFila(m.getId(), m.getNombre(), m.isActivo()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarcaAdminFila crear(@Valid @RequestBody AltaMarcaRequest request) {
        String nombre = request.nombre().trim();
        if (marcaRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ConflictException("MARCA_DUPLICADA", "Ya existe una marca registrada con el nombre '" + nombre + "'");
        }
        Marca marca = new Marca();
        marca.setNombre(nombre);
        marca.setActivo(true);
        Marca guardada = marcaRepository.save(marca);
        return new MarcaAdminFila(guardada.getId(), guardada.getNombre(), guardada.isActivo());
    }

    @PutMapping("/{id}/estado")
    public MarcaAdminFila cambiarEstado(@PathVariable int id, @Valid @RequestBody EstadoMarcaRequest request) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MARCA_NO_ENCONTRADA", "No se encontró la marca con ID " + id));
        marca.setActivo(request.activo());
        Marca actualizada = marcaRepository.save(marca);
        return new MarcaAdminFila(actualizada.getId(), actualizada.getNombre(), actualizada.isActivo());
    }
}
