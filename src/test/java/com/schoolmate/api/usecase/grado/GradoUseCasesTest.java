package com.schoolmate.api.usecase.grado;

import com.schoolmate.api.dto.response.GradoPageResponse;
import com.schoolmate.api.dto.response.GradoResponse;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.GradoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradoUseCasesTest {

    @Mock
    private GradoRepository gradoRepository;

    @InjectMocks
    private ListarGrados listarGrados;
    @InjectMocks
    private ObtenerGrado obtenerGrado;

    @Test
    void listarGrados_sanitizaParametros() {
        when(gradoRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())));

        GradoPageResponse response = listarGrados.execute(-1, 500, "otro");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gradoRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getSortBy()).isEqualTo("nivel");
        assertThat(response.getSortDir()).isEqualTo("asc");
    }

    @Test
    void listarGrados_conSortDesc_aplicaDireccionDescendente() {
        when(gradoRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(Grado.builder().id(UUID.randomUUID()).nombre("6° Básico").nivel(6).build())));

        GradoPageResponse response = listarGrados.execute(0, 20, "DESC");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gradoRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("nivel").getDirection().name()).isEqualTo("DESC");
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void listarGrados_conParametrosNull_usaDefaults() {
        when(gradoRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(
                List.of(Grado.builder().id(UUID.randomUUID()).nombre("2° Básico").nivel(2).build()),
                PageRequest.of(0, 20),
                1
            )
        );

        GradoPageResponse response = listarGrados.execute(null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gradoRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.getSortBy()).isEqualTo("nivel");
    }

    @Test
    void listarGrados_sizeCero_loAjustaAUno() {
        when(gradoRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(Grado.builder().id(UUID.randomUUID()).nombre("3° Básico").nivel(3).build())));

        listarGrados.execute(0, 0, "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gradoRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void listarGrados_retornaMetadataPaginacion() {
        when(gradoRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(
                List.of(Grado.builder().id(UUID.randomUUID()).nombre("4° Básico").nivel(4).build()),
                PageRequest.of(1, 5),
                11
            )
        );

        GradoPageResponse response = listarGrados.execute(1, 5, "asc");

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(11);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    void obtenerGrado_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(gradoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerGrado.execute(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerGrado_siExiste_retornaResponse() {
        UUID id = UUID.randomUUID();
        Grado grado = Grado.builder().id(id).nombre("5° Básico").nivel(5).build();
        when(gradoRepository.findById(id)).thenReturn(Optional.of(grado));

        GradoResponse response = obtenerGrado.execute(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getNombre()).isEqualTo("5° Básico");
        assertThat(response.getNivel()).isEqualTo(5);
    }
}
