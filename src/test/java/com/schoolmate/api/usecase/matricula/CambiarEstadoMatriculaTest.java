package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoMatriculaTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private CambiarEstadoMatricula useCase;

    @Test
    void execute_conEstadoVacio_lanzaApiException() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), " "))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> {
                ApiException apiEx = (ApiException) ex;
                assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                assertThat(apiEx.getMessage()).isEqualTo("El estado es obligatorio");
            });
    }

    @Test
    void execute_conEstadoInvalido_lanzaApiException() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), "NO_EXISTE"))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> {
                ApiException apiEx = (ApiException) ex;
                assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                assertThat(apiEx.getMessage()).isEqualTo("Estado de matrícula inválido");
            });
    }

    @Test
    void execute_conMatriculaInexistente_lanzaResourceNotFound() {
        UUID matriculaId = UUID.randomUUID();
        when(matriculaRepository.findByIdWithRelaciones(matriculaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(matriculaId, "RETIRADO"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Matrícula no encontrada");
    }

    @Test
    void execute_conTransicionInvalida_lanzaBusinessException() {
        UUID matriculaId = UUID.randomUUID();
        Matricula matricula = matriculaBase(matriculaId, EstadoMatricula.RETIRADO);

        when(matriculaRepository.findByIdWithRelaciones(matriculaId)).thenReturn(Optional.of(matricula));

        assertThatThrownBy(() -> useCase.execute(matriculaId, "TRASLADADO"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede cambiar de RETIRADO a TRASLADADO");
    }

    @Test
    void execute_conTransicionValida_actualizaYPersiste() {
        UUID matriculaId = UUID.randomUUID();
        Matricula matricula = matriculaBase(matriculaId, EstadoMatricula.ACTIVA);

        when(matriculaRepository.findByIdWithRelaciones(matriculaId)).thenReturn(Optional.of(matricula));
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatriculaResponse response = useCase.execute(matriculaId, "RETIRADO");

        assertThat(response.getId()).isEqualTo(matriculaId);
        assertThat(response.getEstado()).isEqualTo("RETIRADO");
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void execute_conEstadoLowerCase_loNormalizaAMayusculas() {
        UUID matriculaId = UUID.randomUUID();
        Matricula matricula = matriculaBase(matriculaId, EstadoMatricula.ACTIVA);

        when(matriculaRepository.findByIdWithRelaciones(matriculaId)).thenReturn(Optional.of(matricula));
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatriculaResponse response = useCase.execute(matriculaId, "retirado");

        assertThat(response.getEstado()).isEqualTo("RETIRADO");
        verify(matriculaRepository).save(matricula);
    }

    private static Matricula matriculaBase(UUID id, EstadoMatricula estado) {
        AnoEscolar ano = AnoEscolar.builder()
            .id(UUID.randomUUID())
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 10))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 15))
            .build();
        Curso curso = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("1° Básico A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(ano)
            .build();
        Alumno alumno = Alumno.builder()
            .id(UUID.randomUUID())
            .rut("12345678-5")
            .nombre("María José")
            .apellido("López-Hernández")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();

        return Matricula.builder()
            .id(id)
            .alumno(alumno)
            .curso(curso)
            .anoEscolar(ano)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .estado(estado)
            .createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .build();
    }
}
