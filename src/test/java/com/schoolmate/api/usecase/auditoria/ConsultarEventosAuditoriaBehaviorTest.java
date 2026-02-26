package com.schoolmate.api.usecase.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.dto.response.EventoAuditoriaPageResponse;
import com.schoolmate.api.entity.EventoAuditoria;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEventosAuditoriaBehaviorTest {

    @Mock
    private EventoAuditoriaRepository auditoriaRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConsultarEventosAuditoria useCase;

    @Test
    void execute_conJsonValido_deserializaObjeto() {
        EventoAuditoria evento = EventoAuditoria.builder()
            .id(UUID.randomUUID())
            .usuarioEmail("admin@test.cl")
            .usuarioRol("ADMIN")
            .metodoHttp("POST")
            .endpoint("/api/alumnos")
            .requestBody("{\"alumnoId\":\"abc\"}")
            .responseStatus(200)
            .createdAt(LocalDateTime.of(2026, 2, 26, 12, 0))
            .build();

        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evento)));

        EventoAuditoriaPageResponse response = useCase.execute(
            null,
            "post",
            "/api/alumnos",
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
            0,
            20
        );

        assertThat(response.getEventos()).hasSize(1);
        assertThat(response.getEventos().get(0).getRequestBody()).isInstanceOf(java.util.Map.class);

        verify(auditoriaRepository).findByFiltros(
            eq(false), any(UUID.class),
            eq(true), eq("POST"),
            eq(true), eq("%/api/alumnos%"),
            eq(true), eq(LocalDate.of(2026, 2, 1).atStartOfDay()),
            eq(true), eq(LocalDate.of(2026, 3, 1).atStartOfDay()),
            any(Pageable.class)
        );
    }

    @Test
    void execute_sinFiltros_usaValoresSentinelaYFlagsEnFalse() {
        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        EventoAuditoriaPageResponse response = useCase.execute(null, null, null, null, null, 1, 10);

        verify(auditoriaRepository).findByFiltros(
            eq(false), eq(UUID.fromString("00000000-0000-0000-0000-000000000000")),
            eq(false), eq("__SIN_FILTRO__"),
            eq(false), eq("__SIN_FILTRO__"),
            eq(false), eq(LocalDateTime.of(1970, 1, 1, 0, 0)),
            eq(false), eq(LocalDateTime.of(3000, 1, 1, 0, 0)),
            any(Pageable.class)
        );
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    void execute_conUsuarioId_aplicaFiltroUsuario() {
        UUID usuarioId = UUID.randomUUID();
        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        useCase.execute(usuarioId, null, null, null, null, 0, 20);

        verify(auditoriaRepository).findByFiltros(
            eq(true), eq(usuarioId),
            eq(false), any(String.class),
            eq(false), any(String.class),
            eq(false), any(LocalDateTime.class),
            eq(false), any(LocalDateTime.class),
            any(Pageable.class)
        );
    }

    @Test
    void execute_conMetodoBlank_noAplicaFiltroMetodo() {
        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        useCase.execute(null, "   ", null, null, null, 0, 20);

        verify(auditoriaRepository).findByFiltros(
            eq(false), any(UUID.class),
            eq(false), eq("__SIN_FILTRO__"),
            eq(false), any(String.class),
            eq(false), any(LocalDateTime.class),
            eq(false), any(LocalDateTime.class),
            any(Pageable.class)
        );
    }

    @Test
    void execute_conEndpointConEspacios_aplicaLikeTrimmed() {
        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        useCase.execute(null, null, "  /api/sistema/hora  ", null, null, 0, 20);

        verify(auditoriaRepository).findByFiltros(
            eq(false), any(UUID.class),
            eq(false), any(String.class),
            eq(true), eq("%/api/sistema/hora%"),
            eq(false), any(LocalDateTime.class),
            eq(false), any(LocalDateTime.class),
            any(Pageable.class)
        );
    }

    @Test
    void execute_proyectaMetadataDePaginacion() {
        EventoAuditoria evento = EventoAuditoria.builder()
            .id(UUID.randomUUID())
            .usuarioEmail("admin@test.cl")
            .usuarioRol("ADMIN")
            .metodoHttp("DELETE")
            .endpoint("/api/admin/dev-clock")
            .responseStatus(200)
            .createdAt(LocalDateTime.of(2026, 2, 26, 14, 0))
            .build();

        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evento), PageRequest.of(2, 5), 13));

        EventoAuditoriaPageResponse response = useCase.execute(null, null, null, null, null, 2, 5);

        assertThat(response.getEventos()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(11);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getCurrentPage()).isEqualTo(2);
    }
}
