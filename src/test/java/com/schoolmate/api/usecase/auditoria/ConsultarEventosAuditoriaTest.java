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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEventosAuditoriaTest {

    @Mock
    private EventoAuditoriaRepository auditoriaRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConsultarEventosAuditoria useCase;

    @Test
    void execute_siRequestBodyNoEsJson_retornaStringRaw() {
        EventoAuditoria evento = EventoAuditoria.builder()
            .id(UUID.randomUUID())
            .usuarioEmail("admin@test.cl")
            .usuarioRol("ADMIN")
            .metodoHttp("POST")
            .endpoint("/api/admin/dev-clock")
            .requestBody("{json-invalido")
            .responseStatus(200)
            .ipAddress("127.0.0.1")
            .createdAt(LocalDateTime.of(2026, 2, 26, 10, 0))
            .build();

        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evento)));

        EventoAuditoriaPageResponse response = useCase.execute(null, null, null, null, null, 0, 20);

        assertThat(response.getEventos()).hasSize(1);
        assertThat(response.getEventos().get(0).getRequestBody()).isEqualTo("{json-invalido");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void execute_siRequestBodyEsNull_retornaNull() {
        EventoAuditoria evento = EventoAuditoria.builder()
            .id(UUID.randomUUID())
            .usuarioEmail("admin@test.cl")
            .usuarioRol("ADMIN")
            .metodoHttp("GET")
            .endpoint("/api/sistema/hora")
            .requestBody(null)
            .responseStatus(200)
            .createdAt(LocalDateTime.of(2026, 2, 26, 11, 0))
            .build();

        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evento)));

        EventoAuditoriaPageResponse response = useCase.execute(null, null, null, null, null, 0, 20);

        assertThat(response.getEventos().get(0).getRequestBody()).isNull();
    }

    @Test
    void execute_siRequestBodyEsBlank_retornaNull() {
        EventoAuditoria evento = EventoAuditoria.builder()
            .id(UUID.randomUUID())
            .usuarioEmail("admin@test.cl")
            .usuarioRol("ADMIN")
            .metodoHttp("PATCH")
            .endpoint("/api/matriculas/1/estado")
            .requestBody("   ")
            .responseStatus(200)
            .createdAt(LocalDateTime.of(2026, 2, 26, 11, 30))
            .build();

        when(auditoriaRepository.findByFiltros(
            any(Boolean.class), any(UUID.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(String.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Boolean.class), any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evento)));

        EventoAuditoriaPageResponse response = useCase.execute(null, null, null, null, null, 0, 20);

        assertThat(response.getEventos().get(0).getRequestBody()).isNull();
    }
}
