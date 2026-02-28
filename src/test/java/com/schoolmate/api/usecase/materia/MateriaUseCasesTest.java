package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MateriaUseCasesTest {

    @Mock
    private MateriaRepository materiaRepository;

    @InjectMocks
    private ActualizarMateria actualizarMateria;
    @InjectMocks
    private CrearMateria crearMateria;
    @InjectMocks
    private ListarMaterias listarMaterias;
    @InjectMocks
    private ObtenerMateria obtenerMateria;

    @Test
    void actualizarMateria_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarMateria.execute(id, new MateriaRequest("Mat", "icon")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearMateria_retornaResponse() {
        MateriaRequest request = new MateriaRequest("Historia", "book");
        Materia materia = materia(UUID.randomUUID(), "Historia", "book");
        when(materiaRepository.save(any(Materia.class))).thenReturn(materia);

        MateriaResponse response = crearMateria.execute(request);

        assertThat(response.getNombre()).isEqualTo("Historia");
        assertThat(response.getIcono()).isEqualTo("book");
    }

    @Test
    void crearMateria_mapeaDatosDelRequest() {
        MateriaRequest request = new MateriaRequest("Biologia", "leaf");
        when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> {
            Materia m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
            m.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
            return m;
        });

        MateriaResponse response = crearMateria.execute(request);

        ArgumentCaptor<Materia> captor = ArgumentCaptor.forClass(Materia.class);
        verify(materiaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Biologia");
        assertThat(captor.getValue().getIcono()).isEqualTo("leaf");
        assertThat(response.getNombre()).isEqualTo("Biologia");
    }

    @Test
    void listarMaterias_aplicaDefaultsYSanitiza() {
        Materia materia = materia(UUID.randomUUID(), "Matematica", "math");
        when(materiaRepository.findByActivoTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(materia)));

        MateriaPageResponse response = listarMaterias.execute(-1, 500, "x", "x");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(materiaRepository).findByActivoTrue(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getSortBy()).isEqualTo("nombre");
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void listarMaterias_conSortPermitidoYAsc_respetaOrden() {
        Materia materia = materia(UUID.randomUUID(), "Lenguaje", "menu_book");
        when(materiaRepository.findByActivoTrue(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(materia), PageRequest.of(0, 5), 1));

        MateriaPageResponse response = listarMaterias.execute(0, 5, "updatedAt", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(materiaRepository).findByActivoTrue(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt").getDirection().name()).isEqualTo("ASC");
        assertThat(response.getSortBy()).isEqualTo("updatedAt");
        assertThat(response.getSortDir()).isEqualTo("asc");
    }

    @Test
    void listarMaterias_conSortDirNull_usaDescPorDefecto() {
        Materia materia = materia(UUID.randomUUID(), "Ciencias", "science");
        when(materiaRepository.findByActivoTrue(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(materia), PageRequest.of(0, 20), 1));

        MateriaPageResponse response = listarMaterias.execute(0, 20, "nombre", null);

        assertThat(response.getSortBy()).isEqualTo("nombre");
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void obtenerMateria_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerMateria_siExiste_retornaResponse() {
        UUID id = UUID.randomUUID();
        Materia materia = materia(id, "Filosofia", "psychology");
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(materia));

        MateriaResponse response = obtenerMateria.execute(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getNombre()).isEqualTo("Filosofia");
    }

    @Test
    void obtenerMateria_siEstaInactiva_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarMateria_happyPath_actualizaDatos() {
        UUID id = UUID.randomUUID();
        Materia existente = materia(id, "Musica", "music_note");
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(existente));
        when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> inv.getArgument(0));

        MateriaResponse response = actualizarMateria.execute(id, new MateriaRequest("Artes", "palette"));

        assertThat(response.getNombre()).isEqualTo("Artes");
        assertThat(response.getIcono()).isEqualTo("palette");
    }

    @Test
    void actualizarMateria_siEstaInactiva_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarMateria.execute(id, new MateriaRequest("Artes", "palette")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Materia materia(UUID id, String nombre, String icono) {
        return Materia.builder()
            .id(id)
            .nombre(nombre)
            .icono(icono)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }
}
