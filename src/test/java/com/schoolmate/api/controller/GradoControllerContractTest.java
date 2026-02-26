package com.schoolmate.api.controller;

import com.schoolmate.api.dto.response.GradoPageResponse;
import com.schoolmate.api.dto.response.GradoResponse;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.grado.ListarGrados;
import com.schoolmate.api.usecase.grado.ObtenerGrado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GradoControllerContractTest {

    @Mock private ListarGrados listarGrados;
    @Mock private ObtenerGrado obtenerGrado;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GradoController controller = new GradoController(listarGrados, obtenerGrado);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void listar_retorna200() throws Exception {
        when(listarGrados.execute(0, 20, "asc")).thenReturn(gradoPageResponse());

        mockMvc.perform(get("/api/grados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("e2f8de2d-c3d8-4d74-ac9f-4f27c7454db2"))
            .andExpect(jsonPath("$.content[0].nombre").value("1° Básico"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortDir").value("asc"));

        verify(listarGrados).execute(0, 20, "asc");
    }

    @Test
    void obtener_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerGrado.execute(id)).thenReturn(gradoResponse());

        mockMvc.perform(get("/api/grados/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("e2f8de2d-c3d8-4d74-ac9f-4f27c7454db2"))
            .andExpect(jsonPath("$.nombre").value("1° Básico"))
            .andExpect(jsonPath("$.nivel").value(1));

        verify(obtenerGrado).execute(id);
    }

    private static GradoPageResponse gradoPageResponse() {
        return GradoPageResponse.builder()
            .content(List.of(gradoResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("nombre")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static GradoResponse gradoResponse() {
        return GradoResponse.builder()
            .id(UUID.fromString("e2f8de2d-c3d8-4d74-ac9f-4f27c7454db2"))
            .nombre("1° Básico")
            .nivel(1)
            .createdAt(LocalDateTime.of(2026, 1, 1, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 8, 0))
            .build();
    }
}
