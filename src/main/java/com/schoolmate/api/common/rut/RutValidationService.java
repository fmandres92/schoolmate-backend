package com.schoolmate.api.common.rut;

import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RutValidationService {

    private final AlumnoRepository alumnoRepository;
    private final ProfesorRepository profesorRepository;
    private final ApoderadoRepository apoderadoRepository;

    public void validarFormatoRut(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new BusinessException("El RUT es obligatorio.");
        }

        if (!rut.matches("^\\d{7,8}-[\\dkK]$")) {
            throw new BusinessException("El RUT ingresado no es válido. Verifica el número y dígito verificador.");
        }

        String[] partes = rut.split("-");
        String cuerpo = partes[0];
        char dvIngresado = partes[1].toUpperCase().charAt(0);
        char dvCalculado = calcularDigitoVerificador(cuerpo);

        if (dvIngresado != dvCalculado) {
            throw new BusinessException("El RUT ingresado no es válido. Verifica el número y dígito verificador.");
        }
    }

    public void validarRutDisponible(String rutNormalizado, TipoPersona tipoActual, String idEntidadActual) {
        if (tipoActual != TipoPersona.ALUMNO && alumnoRepository.existsByRut(rutNormalizado)) {
            throw new ConflictException("Este RUT ya está registrado como alumno en el sistema.");
        }

        if (tipoActual != TipoPersona.PROFESOR && profesorRepository.existsByRut(rutNormalizado)) {
            throw new ConflictException("Este RUT ya está registrado como profesor en el sistema.");
        }

        if (tipoActual != TipoPersona.APODERADO && apoderadoRepository.existsByRut(rutNormalizado)) {
            throw new ConflictException("Este RUT ya está registrado como apoderado en el sistema.");
        }
    }

    private char calcularDigitoVerificador(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }

        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
