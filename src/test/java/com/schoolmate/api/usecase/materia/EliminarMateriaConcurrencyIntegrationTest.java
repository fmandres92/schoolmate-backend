package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.common.time.TimeContext;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.usecase.malla.CrearMallaCurricular;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:eliminar-materia-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
class EliminarMateriaConcurrencyIntegrationTest {

    @Autowired private EliminarMateria eliminarMateria;
    @Autowired private CrearMallaCurricular crearMallaCurricular;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private MallaCurricularRepository mallaCurricularRepository;
    @Autowired private GradoRepository gradoRepository;
    @Autowired private AnoEscolarRepository anoEscolarRepository;

    @MockitoBean
    private AsistenciaClaseRepository asistenciaClaseRepository;

    @BeforeEach
    void setUp() {
        TimeContext.setFixed(LocalDateTime.of(2026, 3, 10, 10, 0));
    }

    @AfterEach
    void tearDown() {
        TimeContext.reset();
    }

    @Test
    void execute_bloqueaCreacionConcurrenteDeDependencia_hastaCompletarSoftDelete() throws Exception {
        Materia materia = materiaRepository.save(Materia.builder()
            .nombre("Ciencias")
            .icono("science")
            .activo(true)
            .build());
        Grado grado = gradoRepository.save(Grado.builder()
            .nombre("1° Medio")
            .nivel(9)
            .build());
        AnoEscolar anoEscolar = anoEscolarRepository.save(AnoEscolar.builder()
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build());

        CountDownLatch lockTomado = new CountDownLatch(1);
        CountDownLatch permitirContinuarDelete = new CountDownLatch(1);

        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(eq(materia.getId()))).thenAnswer(invocation -> {
            lockTomado.countDown();
            assertThat(permitirContinuarDelete.await(3, TimeUnit.SECONDS)).isTrue();
            return 0L;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> deleteFuture = executor.submit(() -> {
                eliminarMateria.execute(materia.getId());
                return null;
            });

            assertThat(lockTomado.await(3, TimeUnit.SECONDS)).isTrue();

            Future<Throwable> crearMallaFuture = executor.submit(() -> {
                try {
                    crearMallaCurricular.execute(
                        anoEscolar.getId(),
                        new MallaCurricularRequest(materia.getId(), grado.getId(), anoEscolar.getId(), 6)
                    );
                    return null;
                } catch (Throwable throwable) {
                    return throwable;
                }
            });

            assertThatThrownBy(() -> crearMallaFuture.get(300, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);

            permitirContinuarDelete.countDown();

            deleteFuture.get(3, TimeUnit.SECONDS);
            Throwable throwable = crearMallaFuture.get(3, TimeUnit.SECONDS);

            assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Materia no encontrada");
            assertThat(materiaRepository.findById(materia.getId())).get().extracting(Materia::getActivo).isEqualTo(false);
            assertThat(mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(materia.getId(), anoEscolar.getId())).isEmpty();
        } finally {
            executor.shutdownNow();
        }
    }
}
