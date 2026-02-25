package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.BloqueRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GuardarJornadaDia {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ClockProvider clockProvider;

    private static final LocalTime HORA_MINIMA = LocalTime.of(7, 0);
    private static final LocalTime HORA_MAXIMA = LocalTime.of(18, 0);

    private static final Map<Integer, String> NOMBRES_DIA = Map.of(
        1, "Lunes",
        2, "Martes",
        3, "Miércoles",
        4, "Jueves",
        5, "Viernes"
    );

    @Transactional
    public JornadaDiaResponse execute(UUID cursoId, Integer diaSemana, JornadaDiaRequest request) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + cursoId));

        if (curso.getAnoEscolar().calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar la jornada de un año escolar cerrado");
        }

        validarDiaSemana(diaSemana);

        List<BloqueRequest> bloques = request.getBloques();
        validarBloques(bloques);

        bloqueHorarioRepository.desactivarBloquesDia(cursoId, diaSemana);

        List<BloqueHorario> nuevosBloques = new ArrayList<>();
        for (BloqueRequest bloqueRequest : bloques) {
            BloqueHorario bloque = BloqueHorario.builder()
                .curso(curso)
                .diaSemana(diaSemana)
                .numeroBloque(bloqueRequest.getNumeroBloque())
                .horaInicio(parseHora(bloqueRequest.getHoraInicio(), bloqueRequest.getNumeroBloque(), "inicio"))
                .horaFin(parseHora(bloqueRequest.getHoraFin(), bloqueRequest.getNumeroBloque(), "fin"))
                .tipo(parseTipo(bloqueRequest.getTipo(), bloqueRequest.getNumeroBloque()))
                .materia(null)
                .profesor(null)
                .activo(true)
                .build();
            nuevosBloques.add(bloque);
        }

        List<BloqueHorario> guardados = bloqueHorarioRepository.saveAll(nuevosBloques);
        return construirJornadaDiaResponse(diaSemana, guardados);
    }

    public JornadaDiaResponse construirJornadaDiaResponse(Integer diaSemana, List<BloqueHorario> bloques) {
        List<BloqueHorarioResponse> bloquesResponse = bloques.stream()
            .map(b -> BloqueHorarioResponse.builder()
                .id(b.getId())
                .numeroBloque(b.getNumeroBloque())
                .horaInicio(b.getHoraInicio().toString())
                .horaFin(b.getHoraFin().toString())
                .tipo(b.getTipo().name())
                .materiaId(b.getMateria() != null ? b.getMateria().getId() : null)
                .materiaNombre(b.getMateria() != null ? b.getMateria().getNombre() : null)
                .materiaIcono(b.getMateria() != null ? b.getMateria().getIcono() : null)
                .profesorId(b.getProfesor() != null ? b.getProfesor().getId() : null)
                .profesorNombre(b.getProfesor() != null
                    ? b.getProfesor().getNombre() + " " + b.getProfesor().getApellido()
                    : null)
                .build())
            .collect(Collectors.toList());

        long totalClase = bloques.stream().filter(b -> b.getTipo() == TipoBloque.CLASE).count();
        String horaInicio = bloques.isEmpty() ? null : bloques.get(0).getHoraInicio().toString();
        String horaFin = bloques.isEmpty() ? null : bloques.get(bloques.size() - 1).getHoraFin().toString();

        return JornadaDiaResponse.builder()
            .diaSemana(diaSemana)
            .nombreDia(NOMBRES_DIA.getOrDefault(diaSemana, "Desconocido"))
            .bloques(bloquesResponse)
            .totalBloquesClase((int) totalClase)
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .build();
    }

    private void validarBloques(List<BloqueRequest> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            throw new BusinessException("Debe enviar al menos un bloque");
        }

        for (int i = 0; i < bloques.size(); i++) {
            Integer esperado = i + 1;
            Integer recibido = bloques.get(i).getNumeroBloque();
            if (!esperado.equals(recibido)) {
                throw new BusinessException("Los números de bloque deben ser secuenciales empezando en 1. Se esperaba "
                    + esperado + " pero se recibió " + recibido);
            }
        }

        int contadorAlmuerzo = 0;
        int contadorClase = 0;
        LocalTime horaFinAnterior = null;

        for (BloqueRequest bloque : bloques) {
            LocalTime inicio = parseHora(bloque.getHoraInicio(), bloque.getNumeroBloque(), "inicio");
            LocalTime fin = parseHora(bloque.getHoraFin(), bloque.getNumeroBloque(), "fin");
            TipoBloque tipo = parseTipo(bloque.getTipo(), bloque.getNumeroBloque());

            if (!fin.isAfter(inicio)) {
                throw new BusinessException("Bloque " + bloque.getNumeroBloque() + ": hora_fin (" +
                    bloque.getHoraFin() + ") debe ser posterior a hora_inicio (" + bloque.getHoraInicio() + ")");
            }

            if (inicio.isBefore(HORA_MINIMA) || fin.isAfter(HORA_MAXIMA)) {
                throw new BusinessException("Bloque " + bloque.getNumeroBloque() +
                    ": fuera del rango permitido (07:00 - 18:00)");
            }

            if (horaFinAnterior != null && !inicio.equals(horaFinAnterior)) {
                throw new BusinessException("Bloque " + bloque.getNumeroBloque() + ": hora_inicio (" +
                    bloque.getHoraInicio() + ") debe ser igual a hora_fin del bloque anterior (" +
                    horaFinAnterior + "). La jornada debe ser continua.");
            }

            horaFinAnterior = fin;

            if (tipo == TipoBloque.ALMUERZO) {
                contadorAlmuerzo++;
            }
            if (tipo == TipoBloque.CLASE) {
                contadorClase++;
            }
        }

        if (contadorAlmuerzo > 1) {
            throw new BusinessException("Solo se permite un bloque de ALMUERZO por día");
        }

        if (contadorClase < 1) {
            throw new BusinessException("Debe haber al menos un bloque de tipo CLASE en el día");
        }

        TipoBloque primerTipo = parseTipo(bloques.get(0).getTipo(), bloques.get(0).getNumeroBloque());
        TipoBloque ultimoTipo = parseTipo(
            bloques.get(bloques.size() - 1).getTipo(),
            bloques.get(bloques.size() - 1).getNumeroBloque());

        if (primerTipo != TipoBloque.CLASE) {
            throw new BusinessException("El primer bloque del día debe ser de tipo CLASE");
        }

        if (ultimoTipo != TipoBloque.CLASE) {
            throw new BusinessException("El último bloque del día debe ser de tipo CLASE");
        }
    }

    private void validarDiaSemana(Integer diaSemana) {
        if (diaSemana == null || diaSemana < 1 || diaSemana > 5) {
            throw new BusinessException("Día de semana inválido: " + diaSemana +
                ". Debe ser entre 1 (Lunes) y 5 (Viernes)");
        }
    }

    private LocalTime parseHora(String valor, Integer numeroBloque, String etiqueta) {
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Bloque " + numeroBloque + ": hora de " + etiqueta +
                " inválida (" + valor + "). Formato esperado HH:mm");
        }
    }

    private TipoBloque parseTipo(String valor, Integer numeroBloque) {
        try {
            return TipoBloque.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Bloque " + numeroBloque +
                ": tipo inválido. Valores permitidos: CLASE, RECREO, ALMUERZO");
        }
    }
}
