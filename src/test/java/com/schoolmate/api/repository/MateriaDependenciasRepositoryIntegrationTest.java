package com.schoolmate.api.repository;

import com.schoolmate.api.common.time.TimeContext;
import com.schoolmate.api.dto.projection.BloquesPorCursoProjection;
import com.schoolmate.api.dto.projection.ProfesorNombreProjection;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:materia-dependencias-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("dev")
class MateriaDependenciasRepositoryIntegrationTest {

    @Autowired private MateriaRepository materiaRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private MallaCurricularRepository mallaCurricularRepository;
    @Autowired private BloqueHorarioRepository bloqueHorarioRepository;
    @Autowired private AsistenciaClaseRepository asistenciaClaseRepository;
    @Autowired private AnoEscolarRepository anoEscolarRepository;
    @Autowired private GradoRepository gradoRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        TimeContext.setFixed(LocalDateTime.of(2026, 3, 10, 9, 0));
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        TimeContext.reset();
    }

    @Test
    void materiaRepository_filtraSoloActivas_yLockLookupRespetaActivo() {
        Materia activaA = materiaRepository.save(materia("Activa A", true));
        Materia inactiva = materiaRepository.save(materia("Inactiva", false));
        Materia activaB = materiaRepository.save(materia("Activa B", true));

        var page = materiaRepository.findByActivoTrue(PageRequest.of(0, 10));

        assertThat(page.getContent())
            .extracting(Materia::getId)
            .contains(activaA.getId(), activaB.getId())
            .doesNotContain(inactiva.getId());
        assertThat(materiaRepository.findByIdAndActivoTrue(activaA.getId())).isPresent();
        assertThat(materiaRepository.findByIdAndActivoTrue(inactiva.getId())).isEmpty();

        List<Materia> bloqueadas = transactionTemplate.execute(status ->
            materiaRepository.findActivasByIdInForUpdate(List.of(inactiva.getId(), activaB.getId(), activaA.getId()))
        );

        assertThat(bloqueadas)
            .extracting(Materia::getId)
            .containsExactlyInAnyOrder(activaA.getId(), activaB.getId());
    }

    @Test
    void dependenciasQueries_yDeleteJoinTable_funcionanContraBdReal() {
        Materia materiaObjetivo = materiaRepository.save(materia("Lenguaje", true));
        Materia otraMateria = materiaRepository.save(materia("Historia", true));

        AnoEscolar ano2025 = anoEscolarRepository.save(anoEscolar(2025));
        AnoEscolar ano2026 = anoEscolarRepository.save(anoEscolar(2026));
        Grado primero = gradoRepository.save(grado("1° Básico", 1));
        Grado segundo = gradoRepository.save(grado("2° Básico", 2));

        Curso cursoA = cursoRepository.save(curso("1° Básico A", "A", primero, ano2026));
        Curso cursoB = cursoRepository.save(curso("2° Básico B", "B", segundo, ano2026));

        profesorRepository.save(profesor("Ana", "Bravo", "ana.bravo@test.cl", "11111111-1", List.of(materiaObjetivo)));
        Profesor profesorMixto = profesorRepository.save(
            profesor("Carlos", "Ruiz", "carlos.ruiz@test.cl", "22222222-2", List.of(materiaObjetivo, otraMateria))
        );

        mallaCurricularRepository.save(malla(materiaObjetivo, primero, ano2025, 6, true));
        mallaCurricularRepository.save(malla(materiaObjetivo, segundo, ano2026, 8, true));
        mallaCurricularRepository.save(malla(materiaObjetivo, primero, ano2026, 4, false));

        BloqueHorario bloqueA1 = bloqueHorarioRepository.save(bloque(cursoA, materiaObjetivo, 1, 1, true));
        bloqueHorarioRepository.save(bloque(cursoA, materiaObjetivo, 1, 2, false));
        bloqueHorarioRepository.save(bloque(cursoB, materiaObjetivo, 2, 1, true));
        bloqueHorarioRepository.save(bloque(cursoB, otraMateria, 2, 2, true));

        asistenciaClaseRepository.save(AsistenciaClase.builder()
            .bloqueHorario(bloqueA1)
            .fecha(LocalDate.of(2026, 3, 11))
            .build());

        List<ProfesorNombreProjection> profesores = profesorRepository.findProfesoresByMateriaId(materiaObjetivo.getId());
        List<MallaCurricular> mallasActivas = mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(materiaObjetivo.getId());
        long totalBloques = bloqueHorarioRepository.countByMateriaId(materiaObjetivo.getId());
        List<BloquesPorCursoProjection> bloquesPorCurso = bloqueHorarioRepository.countBloquesPorCursoByMateriaId(materiaObjetivo.getId());
        long totalAsistencias = asistenciaClaseRepository.countByBloqueHorarioMateriaId(materiaObjetivo.getId());

        assertThat(profesores)
            .extracting(p -> p.getNombre() + " " + p.getApellido())
            .containsExactly("Ana Bravo", "Carlos Ruiz");
        assertThat(mallasActivas).hasSize(2);
        assertThatCode(() -> {
            assertThat(mallasActivas.get(0).getAnoEscolar().getAno()).isEqualTo(2025);
            assertThat(mallasActivas.get(0).getGrado().getNombre()).isEqualTo("1° Básico");
            assertThat(mallasActivas.get(1).getAnoEscolar().getAno()).isEqualTo(2026);
            assertThat(mallasActivas.get(1).getGrado().getNombre()).isEqualTo("2° Básico");
        }).doesNotThrowAnyException();
        assertThat(totalBloques).isEqualTo(3L);
        assertThat(bloquesPorCurso)
            .extracting(BloquesPorCursoProjection::getCursoNombre, BloquesPorCursoProjection::getCantidadBloques)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("1° Básico A", 2L),
                org.assertj.core.groups.Tuple.tuple("2° Básico B", 1L)
            );
        assertThat(totalAsistencias).isEqualTo(1L);

        transactionTemplate.executeWithoutResult(status ->
            profesorRepository.deleteProfesorMateriaByMateriaId(materiaObjetivo.getId())
        );

        assertThat(profesorRepository.findProfesoresByMateriaId(materiaObjetivo.getId())).isEmpty();
        Profesor profesorRecargado = profesorRepository.findByIdWithMaterias(profesorMixto.getId()).orElseThrow();
        assertThat(profesorRecargado.getMaterias())
            .extracting(Materia::getNombre)
            .containsExactly("Historia");
    }

    private static Materia materia(String nombre, boolean activo) {
        return Materia.builder()
            .nombre(nombre)
            .icono("book")
            .activo(activo)
            .build();
    }

    private static AnoEscolar anoEscolar(int ano) {
        return AnoEscolar.builder()
            .ano(ano)
            .fechaInicioPlanificacion(LocalDate.of(ano, 1, 1))
            .fechaInicio(LocalDate.of(ano, 3, 1))
            .fechaFin(LocalDate.of(ano, 12, 20))
            .build();
    }

    private static Grado grado(String nombre, int nivel) {
        return Grado.builder()
            .nombre(nombre)
            .nivel(nivel)
            .build();
    }

    private static Curso curso(String nombre, String letra, Grado grado, AnoEscolar anoEscolar) {
        return Curso.builder()
            .nombre(nombre)
            .letra(letra)
            .grado(grado)
            .anoEscolar(anoEscolar)
            .activo(true)
            .build();
    }

    private static Profesor profesor(
        String nombre,
        String apellido,
        String email,
        String rut,
        List<Materia> materias
    ) {
        return Profesor.builder()
            .rut(rut)
            .nombre(nombre)
            .apellido(apellido)
            .email(email)
            .telefono("+56912345678")
            .fechaContratacion(LocalDate.of(2024, 3, 1))
            .horasPedagogicasContrato(30)
            .materias(materias)
            .activo(true)
            .build();
    }

    private static MallaCurricular malla(
        Materia materia,
        Grado grado,
        AnoEscolar anoEscolar,
        int horasPedagogicas,
        boolean activo
    ) {
        return MallaCurricular.builder()
            .materia(materia)
            .grado(grado)
            .anoEscolar(anoEscolar)
            .horasPedagogicas(horasPedagogicas)
            .activo(activo)
            .build();
    }

    private static BloqueHorario bloque(Curso curso, Materia materia, int diaSemana, int numeroBloque, boolean activo) {
        LocalTime horaInicio = LocalTime.of(8 + numeroBloque, 0);
        return BloqueHorario.builder()
            .curso(curso)
            .materia(materia)
            .diaSemana(diaSemana)
            .numeroBloque(numeroBloque)
            .horaInicio(horaInicio)
            .horaFin(horaInicio.plusMinutes(45))
            .tipo(TipoBloque.CLASE)
            .activo(activo)
            .build();
    }
}
