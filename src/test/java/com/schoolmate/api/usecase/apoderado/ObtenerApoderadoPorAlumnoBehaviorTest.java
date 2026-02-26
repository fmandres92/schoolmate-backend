package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.response.ApoderadoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerApoderadoPorAlumnoBehaviorTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ObtenerApoderadoPorAlumno useCase;

    @Test
    void execute_sinVinculos_retornaVacio() {
        UUID alumnoId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId)).thenReturn(List.of());

        Optional<ApoderadoResponse> response = useCase.execute(alumnoId);

        assertThat(response).isEmpty();
        verifyNoInteractions(apoderadoRepository, usuarioRepository);
    }

    @Test
    void execute_conVinculos_retornaDetalleConCuentaYAlumnos() {
        UUID alumnoId = UUID.randomUUID();
        UUID alumno2Id = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.of(
            Apoderado.builder()
                .id(apoderadoId)
                .nombre("Carlos")
                .apellido("Torres")
                .rut("12345678-5")
                .email("carlos@test.cl")
                .telefono("+56911112222")
                .build()
        ));
        when(apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderadoId))
            .thenReturn(List.of(
                vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez")),
                vinculo(apoderadoId, alumno2Id, alumno(alumno2Id, "Luis", "Rojas"))
            ));
        when(usuarioRepository.findByApoderadoId(apoderadoId)).thenReturn(Optional.of(
            Usuario.builder()
                .id(usuarioId)
                .activo(true)
                .build()
        ));

        Optional<ApoderadoResponse> responseOpt = useCase.execute(alumnoId);

        assertThat(responseOpt).isPresent();
        ApoderadoResponse response = responseOpt.orElseThrow();
        assertThat(response.getId()).isEqualTo(apoderadoId);
        assertThat(response.getNombre()).isEqualTo("Carlos");
        assertThat(response.isCuentaActiva()).isTrue();
        assertThat(response.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(response.getAlumnos()).hasSize(2);
        assertThat(response.getAlumnos()).extracting(ApoderadoResponse.AlumnoResumen::getNombre)
            .containsExactly("Ana", "Luis");
    }

    @Test
    void execute_siApoderadoNoExiste_lanzaNotFound() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(alumnoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_sinUsuarioAsociado_retornaCuentaInactiva() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.of(
            Apoderado.builder()
                .id(apoderadoId)
                .nombre("Carla")
                .apellido("Nuñez")
                .rut("12345678-5")
                .email("carla@test.cl")
                .telefono("+56988887777")
                .build()
        ));
        when(apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderadoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(usuarioRepository.findByApoderadoId(apoderadoId)).thenReturn(Optional.empty());

        ApoderadoResponse response = useCase.execute(alumnoId).orElseThrow();

        assertThat(response.getUsuarioId()).isNull();
        assertThat(response.isCuentaActiva()).isFalse();
    }

    @Test
    void execute_conUsuarioInactivo_retornaCuentaInactiva() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.of(
            Apoderado.builder()
                .id(apoderadoId)
                .nombre("Carla")
                .apellido("Nuñez")
                .rut("12345678-5")
                .email("carla@test.cl")
                .telefono("+56988887777")
                .build()
        ));
        when(apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderadoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(usuarioRepository.findByApoderadoId(apoderadoId)).thenReturn(Optional.of(
            Usuario.builder()
                .id(usuarioId)
                .activo(false)
                .build()
        ));

        ApoderadoResponse response = useCase.execute(alumnoId).orElseThrow();

        assertThat(response.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(response.isCuentaActiva()).isFalse();
    }

    @Test
    void execute_filtraAlumnosNulosEnResumen() {
        UUID alumnoId = UUID.randomUUID();
        UUID alumno2Id = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.of(
            Apoderado.builder()
                .id(apoderadoId)
                .nombre("Carlos")
                .apellido("Torres")
                .rut("12345678-5")
                .email("carlos@test.cl")
                .telefono("+56911112222")
                .build()
        ));
        when(apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderadoId))
            .thenReturn(List.of(
                vinculo(apoderadoId, alumnoId, alumno(alumnoId, "Ana", "Perez")),
                vinculo(apoderadoId, alumno2Id, null)
            ));
        when(usuarioRepository.findByApoderadoId(apoderadoId)).thenReturn(Optional.empty());

        ApoderadoResponse response = useCase.execute(alumnoId).orElseThrow();

        assertThat(response.getAlumnos()).hasSize(1);
        assertThat(response.getAlumnos().getFirst().getNombre()).isEqualTo("Ana");
    }

    @Test
    void execute_conMultiplesVinculosIniciales_tomaPrimerApoderado() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderado1Id = UUID.randomUUID();
        UUID apoderado2Id = UUID.randomUUID();

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId))
            .thenReturn(List.of(
                vinculo(apoderado1Id, alumnoId, alumno(alumnoId, "Ana", "Perez")),
                vinculo(apoderado2Id, alumnoId, alumno(alumnoId, "Ana", "Perez"))
            ));
        when(apoderadoRepository.findById(apoderado1Id)).thenReturn(Optional.of(
            Apoderado.builder()
                .id(apoderado1Id)
                .nombre("Primero")
                .apellido("Uno")
                .rut("11111111-1")
                .email("uno@test.cl")
                .telefono("+56911111111")
                .build()
        ));
        when(apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderado1Id))
            .thenReturn(List.of(vinculo(apoderado1Id, alumnoId, alumno(alumnoId, "Ana", "Perez"))));
        when(usuarioRepository.findByApoderadoId(apoderado1Id)).thenReturn(Optional.empty());

        ApoderadoResponse response = useCase.execute(alumnoId).orElseThrow();

        assertThat(response.getId()).isEqualTo(apoderado1Id);
        assertThat(response.getNombre()).isEqualTo("Primero");
    }

    private static ApoderadoAlumno vinculo(UUID apoderadoId, UUID alumnoId, Alumno alumno) {
        return ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
            .alumno(alumno)
            .build();
    }

    private static Alumno alumno(UUID alumnoId, String nombre, String apellido) {
        return Alumno.builder()
            .id(alumnoId)
            .nombre(nombre)
            .apellido(apellido)
            .rut(alumnoId.toString())
            .fechaNacimiento(LocalDate.of(2015, 1, 10))
            .activo(true)
            .build();
    }
}
