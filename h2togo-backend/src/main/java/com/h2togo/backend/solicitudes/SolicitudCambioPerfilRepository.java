package com.h2togo.backend.solicitudes;

import com.h2togo.backend.common.enums.EstadoSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCambioPerfilRepository extends JpaRepository<SolicitudCambioPerfil, Integer> {

    List<SolicitudCambioPerfil> findByIdNegocio(Integer idNegocio);

    List<SolicitudCambioPerfil> findByEstado(EstadoSolicitud estado);
}
